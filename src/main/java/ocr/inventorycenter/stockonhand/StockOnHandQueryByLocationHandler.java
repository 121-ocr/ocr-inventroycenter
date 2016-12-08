package ocr.inventorycenter.stockonhand;


import java.util.ArrayList;
import java.util.List;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.FindOptions;
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
public class StockOnHandQueryByLocationHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "querylocations";

	public StockOnHandQueryByLocationHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
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

	public void getLocationsByRule(JsonObject params, Handler<AsyncResult<JsonArray>> next) {
		FindOptions findOptions = new FindOptions();

		JsonObject sortFields = new JsonObject();
		sortFields.put(StockOnHandConstant.onhandnum, 1);//从小到大排序
		findOptions.setSort(sortFields);

		Future<JsonArray> future = Future.future();
		future.setHandler(next);
		JsonArray los= new JsonArray();
		
		appActivity.getAppDatasource().getMongoClient().findWithOptions(
				appActivity.getDBTableName(appActivity.getBizObjectType()), params.getJsonObject("queryObj"),
				findOptions, result -> {
					if (result.succeeded()) {
						
						// 根据传入依次匹配多个货位
						
						Double pickoutnum = params.getJsonObject("params").getDouble("quantity_should");
						Double allmatchnum= 0.0;
						result.result().forEach( re ->{
							
							Double onhandnum = re.getDouble("onhandnum");
							if(onhandnum>=pickoutnum){//完全匹配
								
								los.add(re);
								future.complete(new JsonArray(result.result()));
							}
							else{
								
								los.add(re);
							    Double newnum=	allmatchnum+onhandnum;
							    allmatchnum=   newnum;
							   
							   
							}
							
							
						});
						
						
						
						future.complete(new JsonArray(result.result()));

					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						// event.fail(100, errMsgString);
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
