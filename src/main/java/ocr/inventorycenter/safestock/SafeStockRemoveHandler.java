package ocr.inventorycenter.safestock;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleSingleDocBaseHandler;
import ocr.common.handler.SampleSingleDocRemoveHandler;
import ocr.inventorycenter.invarea.InvAreaConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 库存中心：安全库存-删除
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class SafeStockRemoveHandler extends SampleSingleDocRemoveHandler {
	
	public static final String ADDRESS = SafeStockConstant.RemoveAddressConstant;
	
	public SafeStockRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 此action的入口地址
	 */
	@Override
	public String getEventAddress() {
		return ADDRESS;
	}

	/**
	 * {@inheritDoc} 此action的自描述元数据
	 */
	@Override
	public ActionDescriptor getActionDesc() {		
		
		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, InvAreaConstant.CREATE_STATUS, InvAreaConstant.REMOVE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
	
}
