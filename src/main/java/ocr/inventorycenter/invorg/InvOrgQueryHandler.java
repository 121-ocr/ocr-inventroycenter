package ocr.inventorycenter.invorg;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存组织规划：对象（仓库档案）-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class InvOrgQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query";

	public InvOrgQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	//处理器
	@Override
	public void handle(OtoCloudBusMessage<JsonObject> msg) {
		
		JsonObject query = msg.body();
		
		appActivity.getAppDatasource().getMongoClient().find(appActivity.getDBTableName(this.appActivity.getBizObjectType()), 
				query, findRet->{
					if (findRet.succeeded()) {
						msg.reply(findRet.result());
					} else {
						Throwable err = findRet.cause();
						String errMsg = err.getMessage();
						appActivity.getLogger().error(errMsg, err);
						msg.fail(500, errMsg);
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
