package ocr.inventorycenter.stockout;

import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleBillBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;

/**
 * 拣货出库单--（拣货态-->发货状）
 * 
 * @author LCL
 *
 */
public class StockOutShippingHandler extends SampleBillBaseHandler{

	public static final String ADDRESS = StockOutConstant.ShippingOutAddressConstant;

	public StockOutShippingHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
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
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, StockOutConstant.CreatedStatus, StockOutConstant.PickOutedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}