package ocr.inventorycenter.invfacility;


import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import otocloud.common.ActionURI;
import otocloud.common.OtoCloudDirectoryHelper;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.ActionHandlerImpl;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
import otocloud.framework.core.OtoCloudBusMessage;
/**
 * 库存中心：采购入库-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class InvFacilityQueryHandler extends ActionHandlerImpl<JsonObject> {
	
	public static final String ADDRESS = "query";

	public InvFacilityQueryHandler(AppActivityImpl appActivity) {
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

		
		String menusFilePath = OtoCloudDirectoryHelper.getConfigDirectory() + "invfacility.json";		
				
		this.getAppActivity().getVertx().fileSystem().readFile(menusFilePath, result -> {
    	    if (result.succeeded()) {
    	    	String fileContent = result.result().toString(); 
    	        
    	    	JsonArray srvCfg = new JsonArray(fileContent);
    	        msg.reply(srvCfg);     	        
    	        
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
				
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.GET);
		handlerDescriptor.setRestApiURI(uri);
		
		
		return actionDescriptor;
	}
	
	
}
