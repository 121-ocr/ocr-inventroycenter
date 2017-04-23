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
import otocloud.framework.core.CommandMessage;
/**
 * 查询当前租户下所有的存货（按照保质期排序）
 * @author pcitc
 *
 */
public class StockOnHandQuery4ShelfWarningHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query4shelfwarning";

	public StockOnHandQuery4ShelfWarningHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}
	
	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject params = msg.getContent();
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
	 * 		query: { account: "3"},
	 * 		status: ["IN","OUT","RES"]
	 * 		group_keys： ["warehousecode","sku", "invbatchcode", "locationcode"],
	 * 		sort: { shelf_life: 1 },
	 * 		need_goods: true
	 * }
	 * 
	 * 使用了分组计算批次+货位存量
	 * db.bs_stockonhand_3.aggregate(
		   [
		      {
		          $match : { "status": {"$in": ["IN","OUT","RES"]}}
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

		JsonObject query = new JsonObject();
		query = new JsonObject().put("status", new JsonObject().put("$in", new JsonArray()
				.add("IN")
				.add("OUT")
				.add("RES")));
		
		JsonObject groupIds = new JsonObject();
		groupIds.put("warehousecode", "$warehousecode");
		groupIds.put("sku", "$sku");	
		groupIds.put("invbatchcode", "$invbatchcode");	
		groupIds.put("shelf_life", "$shelf_life");
		groupIds.put("goods", "$goods");
		groupIds.put("warehouses", "$warehouses");

		JsonObject matchObj = new JsonObject().put("$match", query);
		
	
		JsonObject groupComputeFields = new JsonObject()
											.put("_id", groupIds)
											.put("onhandnum", new JsonObject().put("$sum", "$onhandnum"));
		
		JsonObject sortObj = new JsonObject();
		sortObj.put("$sort", new JsonObject().put("shelf_life", 1));	
		
		JsonObject groupObj = new JsonObject().put("$group", groupComputeFields);
		
		JsonArray piplelineArray = new JsonArray();
		piplelineArray.add(matchObj).add(groupObj).add(sortObj);
		
		JsonObject command = new JsonObject()
								  .put("aggregate", appActivity.getDBTableName(appActivity.getBizObjectType()))
								  .put("pipeline", piplelineArray);

		appActivity.getAppDatasource().getMongoClient().runCommand("aggregate", command, result -> {
		if (result.succeeded()) {
				JsonArray stockOnHandRet = result.result().getJsonArray("result");    	
				future.complete(stockOnHandRet);    
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
