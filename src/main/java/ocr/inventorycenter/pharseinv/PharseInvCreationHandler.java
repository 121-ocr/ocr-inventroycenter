package ocr.inventorycenter.pharseinv;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleBillBaseHandler;
import ocr.inventorycenter.stockout.StockOutConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 库存中心：采购入库-创建
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class PharseInvCreationHandler extends SampleBillBaseHandler {

	public static final String ADDRESS = PharseInvConstant.CreateAddressConstant;

	public PharseInvCreationHandler(AppActivityImpl appActivity) {
		super(appActivity);
	
	}

	/**
	 * corecorp_setting.setting
	 */
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

		// 外部访问url定义
		ActionURI uri = new ActionURI(ADDRESS, HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, StockOutConstant.CreatedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
