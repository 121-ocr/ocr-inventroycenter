package ocr.inventorycenter.stockonhand;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;


/**
 * 库存中心：现存量-先进先出（FIFO）查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandQueryByFIFOHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query_fifo";

	public StockOnHandQueryByFIFOHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		getLocationsBatchByRule(event.body(), ret -> {
			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}
	
	/**
	 * // 先进先出分组规则：仓库+SKU+批次+货位
	   // 先进先出排序规则：先批次，后货位存量
	   // 条件：仓库、SKU
	 * 输入参数：{
	 * 		query: { warehousecode: "WH001", sku: "WINE001SP0001SP0001"},
	 * 		req_param: {"res_bid": "xxxx", "quantity_should": 200}, 
	 * }
	 * 
	 * @param params
	 * @param next
	 */
	private void getLocationsBatchByRule(JsonObject params, Handler<AsyncResult<JsonArray>> next) {

		Future<JsonArray> future = Future.future();
		future.setHandler(next);		

		params.put("status", new JsonArray()
				.add("IN")
				.add("OUT")
				.add("RES"));
		
		params.put("group_keys", new JsonArray()
				.add("warehousecode")
				.add("sku")
				.add("invbatchcode")
				.add("shelf_life")
				.add("locationcode"));
		
		params.put("sort",  new JsonObject()
								.put("invbatchcode", 1)
								.put("onhandnum", 1));
		
		//调用现存量查询服务
		StockOnHandQueryHandler stockOnHandQueryHandler = new StockOnHandQueryHandler(this.appActivity);
			stockOnHandQueryHandler.queryOnHand(params, result -> {
					if (result.succeeded()) {
					
						JsonArray stockOnHandRet = result.result();
						
						JsonObject reqParam = params.getJsonObject("req_param");
						
						// 根据传入依次匹配多个货位
						Double pickoutnum = reqParam.getDouble("quantity_should");

						JsonObject sum = new JsonObject();
						sum.put("sum", 0.0);
						
						JsonArray allresults = new JsonArray();

						String boId = reqParam.getString("res_bid");// 来源id
						
						for(Object item : stockOnHandRet) {
							JsonObject stockOnHandItem = (JsonObject)item;
							
							Double onhandnum = stockOnHandItem.getDouble("onhandnum");
							JsonObject _idFeilds = stockOnHandItem.getJsonObject("_id");
							
							//否则，计算累加量
							Double sumNum = sum.getDouble("sum");
							Double newSumNum = sumNum + onhandnum;	
							//累加量是否满足
							if(newSumNum.compareTo(pickoutnum) >= 0){
								// 多货位累加匹配
								addToPickOutLocations(boId, _idFeilds, pickoutnum - sumNum, allresults);
								break;
							}							
							
							//否则，将当前货位量加入拣货列表
							addToPickOutLocations(boId, _idFeilds, onhandnum, allresults);
							
							//设置累加器，开始下一个货位迭代
							sum.put("sum", newSumNum);
						}
						
						future.complete(allresults);

					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						// event.fail(100, errMsgString);
						future.fail(errThrowable);

					}
				});
		
		
	}
	
	private void addToPickOutLocations(String boId, JsonObject re, Double surplus_onhand, JsonArray allresults) {
		JsonObject newres = new JsonObject();
		newres.put("bid", boId);
		newres.put("shelf_life", re.getValue("shelf_life"));
		//newres.put("onhandid", ((JsonObject) re.getValue("_id")).getString("$oid")); //因为是现存量是流水写入，故不需要返回_id
		newres.put("invbatchcode", re.getValue("invbatchcode"));
		newres.put("locationcode", re.getValue("locationcode"));
		newres.put("surplus_onhand", surplus_onhand);
		allresults.add(newres);
	}



	/**
	 * 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);
		
		
		return actionDescriptor;
	}
	
	
}
