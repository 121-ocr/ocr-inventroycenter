package ocr.inventorycenter.stockonhand;


import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
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
 * 库存中心：现存量-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query";

	public StockOnHandQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}
	
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		JsonObject params = msg.body();
		queryOnHand(params, ret->{
			if (ret.succeeded()) {
				msg.reply(ret.result());
			}else{
				Throwable errThrowable = ret.cause();
				String errMsgString = errThrowable.getMessage();
				msg.fail(100, errMsgString);
			}
		});
	}

	/**
	 * 输入参数：{
	 * 		query: { warehousecode: "WH001", sku: "WINE001SP0001SP0001", invbatchcode: "1"},
	 * 		status: ["IN","OUT","RES"]
	 * 		group_keys： ["warehousecode","sku", "invbatchcode", "locationcode"],
	 * 		sort: { invbatchcode: 1, onhandnum: 1 },
	 * 		need_goods: true
	 * }
	 * 
	 * 使用了分组计算批次+货位存量
	 * db.bs_stockonhand_3.aggregate(
		   [
		      {
		          $match : { "warehousecode": "WH001", "sku": "WINE001SP0001SP0001", "status": {"$in": ["IN","OUT","RES"]}}
		      },
		      {
		        $group : {
		           _id : { "warehousecode": "$warehousecode", "sku": "$sku", "invbatchcode": "$invbatchcode", "shelf_life": "$shelf_life", "locationcode": "$locationcode"},
		           shelf_life: {$first: "$shelf_life" },
		           invbatchcode: {$first: "$invbatchcode" },
		           onhandnum: {$sum: "$onhandnum" }
		        }
		      },
		      {
		        $sort: {
		          shelf_life: 1,
		          invbatchcode: 1,
		          onhandnum: 1 
		        }
		      }
		   ]
		)	
	 * @param params
	 * @param next
	 */
	public void queryOnHand(JsonObject params,  Handler<AsyncResult<JsonArray>> next){
		
		Future<JsonArray> future = Future.future();
		future.setHandler(next);

		
		String account = this.appActivity.getAppInstContext().getAccount();
		
		JsonArray status = params.getJsonArray("status");
		
		String _goodsAccount = "";		
		JsonObject query = params.getJsonObject("query");
		if(query != null){
			if(query.containsKey("goodaccount")){
				_goodsAccount = query.getString("goodaccount");
			}
			if(status != null && status.size() > 0){
				query.put("status", new JsonObject().put("$in", status));
			}
			else {
				query.put("status", new JsonObject().put("$in", new JsonArray()
																	.add("IN")
																	.add("OUT")
																	.add("RES")));
			}
		}else{
			if(status != null && status.size() > 0){
				query = new JsonObject().put("status", new JsonObject().put("$in", status));
			}
			else {
				query = new JsonObject().put("status", new JsonObject().put("$in", new JsonArray()
																						.add("IN")
																						.add("OUT")
																						.add("RES")));
			}
			
		}
		final String goodsAccount = _goodsAccount;
		
		Boolean _needGoods = false;
		if(params.containsKey("need_goods")){
			_needGoods = params.getBoolean("need_goods");			
		}		
		final Boolean needGoods = _needGoods;
		
		Boolean _hasGoodsAccountGroup = false;		
		JsonArray groupKeys = params.getJsonArray("group_keys");
		JsonObject groupIds = new JsonObject();
		for(Object item : groupKeys){
			String groupKey = (String)item;
			String groupVal = "$" + groupKey; //mongodb的aggregate命令语法
			groupIds.put(groupKey, groupVal);	
			
			if(groupKey.equals("goodaccount")){
				_hasGoodsAccountGroup = true;
			}
		}
		final Boolean hasGoodsAccountGroup = _hasGoodsAccountGroup;
		
		JsonObject matchObj = new JsonObject().put("$match", query);
		
	
		JsonObject groupComputeFields = new JsonObject()
											.put("_id", groupIds)
											.put("onhandnum", new JsonObject().put("$sum", "$onhandnum"));
		
		JsonObject sortObj = new JsonObject();
		JsonObject sortParam = params.getJsonObject("sort");
		if(sortParam == null){
			sortObj.put("$sort", new JsonObject().put("onhandnum", 1));
		}else{
			sortObj.put("$sort", sortParam);
			sortParam.forEach(action->{
				String key = action.getKey(); 
				if(!key.equals("onhandnum")){
					groupComputeFields.put(key, new JsonObject().put("$first", "$" + key));
				}
			});
		}		
		
		JsonObject groupObj = new JsonObject().put("$group", groupComputeFields);
		
		JsonArray piplelineArray = new JsonArray();
		piplelineArray.add(matchObj).add(groupObj).add(sortObj);
		
		JsonObject command = new JsonObject()
								  .put("aggregate", appActivity.getDBTableName(appActivity.getBizObjectType()))
								  .put("pipeline", piplelineArray);

		appActivity.getAppDatasource().getMongoClient().runCommand("aggregate", command, result -> {
		if (result.succeeded()) {
				JsonArray stockOnHandRet = result.result().getJsonArray("result");    	  
				int skSize = stockOnHandRet.size();
				if(skSize > 0){
					List<Future> futures = new ArrayList<Future>();
					
					String goodsSrvName = this.appActivity.getDependencies().getJsonObject("goods_service").getString("service_name","");
					
					String _goodsAddress = account + "." + goodsSrvName + "." + "goods-mgr.findone";	
					if(goodsAccount != null && !goodsAccount.isEmpty()){
						_goodsAddress = goodsAccount + "." + goodsSrvName + "." + "goods-mgr.findone";		
					}
					final String goodsAddress = _goodsAddress;
					
					//List<Integer> removedItems = new ArrayList<Integer>();
					
					JsonArray retArray = new JsonArray();
					
					for(int i=0; i<skSize; i++){
						JsonObject stockOnHandItem = stockOnHandRet.getJsonObject(i);
						if(stockOnHandItem.getDouble("onhandnum").compareTo(new Double("0.00")) == 0){
							//removedItems.add(i);
						}else{	
							retArray.add(stockOnHandItem);
							if(needGoods){
							
								Future<JsonObject> returnFuture = Future.future();
								futures.add(returnFuture);
								
								String goodsSrvAddr = goodsAddress;
								if(hasGoodsAccountGroup){
									goodsSrvAddr = stockOnHandItem.getJsonObject("_id").getString("goodaccount") + "." + goodsSrvName + "." + "goods-mgr.findone";	
								}
								
								this.appActivity.getEventBus().send(goodsSrvAddr, 
										new JsonObject().put("product_sku_code", stockOnHandItem.getJsonObject("_id").getString("sku")), goodsRet->{
									if(goodsRet.succeeded()){
										JsonObject goodsRetObj = (JsonObject)goodsRet.result().body();
										JsonObject goods = goodsRetObj.getJsonArray("result").getJsonObject(0);
										stockOnHandItem.put("goods", goods);		
										returnFuture.complete();
									}else{
										Throwable errThrowable = goodsRet.cause();
										String errMsgString = errThrowable.getMessage();
										appActivity.getLogger().error(errMsgString, errThrowable);
										returnFuture.fail(errThrowable);
									}
								});
							}
						}
					}	
/*					if(removedItems.size() > 0){
						for(Integer idx: removedItems){
							stockOnHandRet.remove(idx.intValue());
						}
					}*/
					if(needGoods){
						CompositeFuture.join(futures).setHandler(ar -> {
							future.complete(retArray);
						});
					}else{
						future.complete(retArray);
					}
				}else{				
					future.complete(stockOnHandRet);    
				}
    	    } else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				future.fail(errThrowable);		
   
    	    }	
		});

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
