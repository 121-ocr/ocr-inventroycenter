package ocr.inventorycenter.locationrelation;


import io.vertx.core.http.HttpMethod;
import ocr.common.handler.SampleSingleDocBaseHandler;
import otocloud.common.ActionURI;
import otocloud.framework.app.function.ActionDescriptor;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRootType;
import otocloud.framework.app.function.BizStateSwitchDesc;
import otocloud.framework.core.HandlerDescriptor;
/**
 * 货位商品关系档案
 * 
 * @date 2016年11月20日
 * @author LCL
 */
//业务活动功能处理器
public class LocationsGoodsRelationCreateHandler  extends SampleSingleDocBaseHandler {
	
	public LocationsGoodsRelationCreateHandler(AppActivityImpl appActivity) {
		super(appActivity);
	}

	/**
	 * corecorp_setting.setting
	 */
	@Override 
	public String getEventAddress() {
		return LocationsGoodsRelationConstant.CREATE_ADDRESS;
	}

	@Override
	public ActionDescriptor getActionDesc() {

		ActionDescriptor actionDescriptor = super.getActionDesc();
		HandlerDescriptor handlerDescriptor = actionDescriptor.getHandlerDescriptor();

		// 外部访问url定义
		ActionURI uri = new ActionURI(getEventAddress(), HttpMethod.POST);
		handlerDescriptor.setRestApiURI(uri);

		// 状态变化定义
		BizStateSwitchDesc bizStateSwitchDesc = new BizStateSwitchDesc(BizRootType.BIZ_OBJECT, null, LocationsGoodsRelationConstant.CREATE_STATUS);
		bizStateSwitchDesc.setWebExpose(true); // 是否向web端发布事件
		actionDescriptor.setBizStateSwitch(bizStateSwitchDesc);

		return actionDescriptor;
	}
	
	
}