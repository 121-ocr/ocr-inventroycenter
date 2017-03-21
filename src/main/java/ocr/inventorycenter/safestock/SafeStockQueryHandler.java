package ocr.inventorycenter.safestock;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleDocQueryHandler;
import ocr.common.handler.SampleSingleDocQueryHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 库存中心：安全库存-查询
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class SafeStockQueryHandler extends SampleSingleDocQueryHandler {
	
	public static final String ADDRESS = SafeStockConstant.QueryAddressConstant;

	public SafeStockQueryHandler(AppActivityImpl appActivity) {
		super(appActivity);
		
	}

	//此action的入口地址
	@Override
	public String getEventAddress() {
		return ADDRESS;
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
