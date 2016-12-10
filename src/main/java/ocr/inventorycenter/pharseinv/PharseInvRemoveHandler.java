package ocr.inventorycenter.pharseinv;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleDocBaseHandler;
import ocr.inventorycenter.stockreserved.StockReservedConstant;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 库存中心：采购入库-删除
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class PharseInvRemoveHandler extends SampleDocBaseHandler {

	public static final String ADDRESS = PharseInvConstant.RemoveAddressConstant;

	public PharseInvRemoveHandler(AppActivityImpl appActivity) {
		super(appActivity);
		// TODO Auto-generated constructor stub
	}

	// 此action的入口地址
	@Override
	public String getEventAddress() {
		// TODO Auto-generated method stub
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

		// 状态变化定义，注意，这个目前是还创建态，因为这个解除不是删除，而是增加一条负数据
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT,
				StockReservedConstant.CreatedStatus, StockReservedConstant.CreatedStatus);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}

}
