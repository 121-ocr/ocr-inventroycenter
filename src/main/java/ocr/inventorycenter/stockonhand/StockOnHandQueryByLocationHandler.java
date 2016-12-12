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
 * 库存中心：现存量-查询：传入批次，匹配货位
 * 
 * @date 2016年11月20日
 * @author LCL
 */
// 业务活动功能处理器
public class StockOnHandQueryByLocationHandler extends ActionHandlerImpl<JsonObject> {

	public static final String ADDRESS = "querylocations";

	public StockOnHandQueryByLocationHandler(AppActivityImpl appActivity) {
		super(appActivity);

	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	@Override
	public void handle(OtoCloudBusMessage<JsonObject> event) {

		getLocationsByRule(event.body(), ret -> {
			
			if (ret.succeeded()) {
				event.reply(ret.result());
			} else {
				Throwable errThrowable = ret.cause();
				event.fail(100, errThrowable.getMessage());
			}

		});

	}
	
	/**
	 * 输入参数：{
	 * 		query: { warehousecode: "WH001", sku: "WINE001SP0001SP0001", invbatchcode: "1"},
	 * 		groupKeys： ["warehousecode","sku", "invbatchcode", "locationcode"],
	 * 		params: {"res_bid": "xxxx", "quantity_should": 200}, 
	 * }
	 * 
	 * 使用了分组计算批次+货位存量
	 * db.bs_stockonhand_3.aggregate(
		   [
		      {
		          $match : { warehousecode: "WH001", sku: "WINE001SP0001SP0001", invbatchcode: "1"}
		      },
		      {
		        $group : {
		           _id : { warehousecode: "$warehousecode", sku: "$sku", invbatchcode: "$invbatchcode", locationcode: "$locationcode" },
		           onhandnum: { $sum: "$onhandnum" }
		        }
		      },
		      {
		        $sort: {
		          onhandnum: 1 
		        }
		      }
		   ]
		)
	 * @param params
	 * @param next
	 */
	public void getLocationsByRule(JsonObject params, Handler<AsyncResult<JsonArray>> next) {

		Future<JsonArray> future = Future.future();
		future.setHandler(next);
		
		JsonObject query = params.getJsonObject("query");
		JsonObject paramsObj = params.getJsonObject("params");
		
		JsonArray groupKeys = params.getJsonArray("groupKeys");
		JsonObject groupIds = new JsonObject();
		groupKeys.forEach(item->{
			String groupKey = (String)item;
			String groupVal = "$" + groupKey; //mongodb的aggregate命令语法
			groupIds.put(groupKey, groupVal);			
		});
		
		JsonObject matchObj = new JsonObject().put("$match", query);
		JsonObject groupObj = new JsonObject().put("$group", new JsonObject()
															.put("_id", groupIds)
															.put("onhandnum", new JsonObject().put("$sum", "$onhandnum")));		
		JsonObject sortObj = new JsonObject().put("$sort", new JsonObject().put("onhandnum", 1));
		
		JsonArray piplelineArray = new JsonArray();
		piplelineArray.add(matchObj).add(groupObj).add(sortObj);
		
		JsonObject command = new JsonObject()
								  .put("aggregate", appActivity.getDBTableName(appActivity.getBizObjectType()))
								  .put("pipeline", piplelineArray);

		appActivity.getAppDatasource().getMongoClient().runCommand("aggregate", command, result -> {
					if (result.succeeded()) {

						JsonArray stockOnHandRet = result.result().getJsonArray("result");
						
						// 根据传入依次匹配多个货位
						Double pickoutnum = paramsObj.getDouble("quantity_should");

						JsonObject sum = new JsonObject();
						sum.put("sum", 0.0);
						
						JsonArray allresults = new JsonArray();

						String boId = paramsObj.getString("res_bid");// 来源id
						
						for(Object item : stockOnHandRet) {
							JsonObject stockOnHandItem = (JsonObject)item;
							
							Double onhandnum = stockOnHandItem.getDouble("onhandnum");
							
							//先匹配当前货位量，如果够全出，则直接出此货位量
							if (onhandnum.compareTo(pickoutnum) == 0) {// 整仓匹配
								allresults = new JsonArray();
								addToPickOutLocations(boId, stockOnHandItem, onhandnum, allresults);
								break;
							}

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
		//newres.put("onhandid", ((JsonObject) re.getValue("_id")).getString("$oid")); //因为是现存量是流水写入，故不需要返回_id
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
