package ocr.inventorycenter.warehouse;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.CommandMessage;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 退货单提交
 * @author pcitc
 *
 */
public class WarehouseOwnerBizUnitGetHandler extends ActionHandlerImpl<JsonObject> {
	
	private static String ADDRESS = "owner-bizunit.get";
	
	public WarehouseOwnerBizUnitGetHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);
		
		return actionDescriptor;
	}
	

	@Override
	public void handle(CommandMessage<JsonObject> msg) {
		
		String acctOrgSrvName = this.appActivity.getDependencies().getJsonObject("otocloud-acct-org_service")
				.getString("service_name", "");
		Long acctId = Long.parseLong(this.appActivity.getAppInstContext().getAccount());
		String getWarehouseAddress = acctOrgSrvName + "." + "my-bizunit.query";
		msg.send(getWarehouseAddress, 
				new JsonObject().put("acct_id", acctId), invRet -> {
			if (invRet.succeeded()) {
				msg.reply(invRet.result().body());
			} else {
				String err = invRet.cause().getMessage();
				this.appActivity.getLogger().error(err, invRet.cause());
				msg.fail(400, err);
			}
		});
		
	}
}
