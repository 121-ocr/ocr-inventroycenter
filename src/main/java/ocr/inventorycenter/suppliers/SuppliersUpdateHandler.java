package ocr.inventorycenter.suppliers;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.CommandMessage;
/**
 * 库存中心：库区-修改
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class SuppliersUpdateHandler  extends ActionHandlerImpl<JsonObject> {
	
	public SuppliersUpdateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override 
	public String getEventAddress() {
		return SuppliersConstant.UPDATE_ADDRESS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		return actionDescriptor;
	}

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		JsonObject supplier = msg.body().getJsonObject("content");    	
		Long supplier_acct = supplier.getLong("supplier_acct", -1L);
		
		Long acctId = Long.parseLong(this.appActivity.getAppInstContext().getAccount());

		this.appActivity.getAppDatasource().getMongoClient_oto().save(
				appActivity.getDBTableName(appActivity.getBizObjectType()), supplier, result -> {
			if (result.succeeded()) {
				
				if(supplier_acct > 0L){				
					String authSrvName = componentImpl.getDependencies().getJsonObject("otocloud-acct").getString("service_name","");
					String address = authSrvName + ".acct-relation.create";
	
					JsonObject acctRelJsonObject = new JsonObject()
							.put("from_acct_id", acctId)
							.put("to_acct_id", supplier_acct)
							.put("desc", "采购关系");
					JsonObject sendMsg = new JsonObject().put("content", acctRelJsonObject);
					
					
					componentImpl.getEventBus().send(address,
							sendMsg, acctRelationRet->{
								if(acctRelationRet.succeeded()){
									msg.reply(result.result());
								}else{		
									Throwable err = acctRelationRet.cause();						
									err.printStackTrace();		
									msg.fail(100, err.getMessage());
								}	
								
					});	
				}else{
					msg.reply(result.result());
				}
				
			} else {
				Throwable errThrowable = result.cause();
				String errMsgString = errThrowable.getMessage();
				appActivity.getLogger().error(errMsgString, errThrowable);
				msg.fail(100, errMsgString);
			}
		});	
		
	}
	
	
}
