package ocr.inventorycenter.stockonhand;


import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
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
public class StockOnHandCreationHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "create";

	public StockOnHandCreationHandler(AppActivityImpl appActivity) {
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
	public void handle(OtoCloudBusMessage<JsonObject> msg) {

		
		MultiMap headerMap = msg.headers();
		
		JsonObject so = msg.body();
		
    	String namecode = so.getString("namecode");
    	String partnerAcct = so.getString("customer"); //交易单据一般要记录协作方    	
    	
		String acctId = this.appActivity.getAppInstContext().getAccount();
		JsonObject settingInfo = msg.body();
		settingInfo.put("account", acctId);
		
		appActivity.getAppDatasource().getMongoClient().save(
				appActivity.getDBTableName(appActivity.getName()), settingInfo,
				result -> {
					if (result.succeeded()) {
						settingInfo.put("_id", result.result());
						msg.reply(settingInfo);						
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
