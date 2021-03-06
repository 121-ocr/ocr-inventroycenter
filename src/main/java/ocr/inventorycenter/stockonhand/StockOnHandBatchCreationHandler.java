package ocr.inventorycenter.stockonhand;


import io.vertx.core.MultiMap;
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
 * 库存中心：现存量-创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class StockOnHandBatchCreationHandler extends ActionHandlerImpl<JsonArray> {
	
	public static final String ADDRESS = "batch-create";

	public StockOnHandBatchCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonArray> msg) {
		
		JsonArray soArray = msg.body();
		for (Object so : soArray) {
			String acctId = this.appActivity.getAppInstContext().getAccount();
			((JsonObject) so).put("account", acctId);
		}
		
		appActivity.getAppDatasource().getMongoClient_oto().save(
				appActivity.getDBTableName(appActivity.getName()), soArray,
				result -> {
					if (result.succeeded()) {
						msg.reply(result.result());						
					} else {
						Throwable errThrowable = result.cause();
						String errMsgString = errThrowable.getMessage();
						appActivity.getLogger().error(errMsgString, errThrowable);
						msg.fail(100, errMsgString);		
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
