package ocr.inventorycenter.pharseorder;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 采购订单
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class PharseOrderManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return PharseOrderConstant.ComponentNameConstant;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return PharseOrderConstant.ComponentBizObjectTypeConstant;
	}


	// 发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		return null;
	}

	// 业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		PharseOrderCreationHandler initHandler = new PharseOrderCreationHandler(this);
		ret.add(initHandler);

		PharseOrderQueryHandler queryHandler = new PharseOrderQueryHandler(this);
		ret.add(queryHandler);

		PharseOrderRemoveHandler removeHandler = new PharseOrderRemoveHandler(this);
		ret.add(removeHandler);

		PharseOrderUpdateHandler updateHandler = new PharseOrderUpdateHandler(this);
		ret.add(updateHandler);

		PharseOrderQueryByConditonHandler pharseOrderQueryByConditonHandler = new PharseOrderQueryByConditonHandler(
				this);
		ret.add(pharseOrderQueryByConditonHandler);

		PharseOrderCreatePharseInvHandler pharseOrderCreatePharseInvHandler = new PharseOrderCreatePharseInvHandler(
				this);
		ret.add(pharseOrderCreatePharseInvHandler);

		PharseOrderQueryNoPharseInvHandler pharseOrderQueryNoPharseInvHandler = new PharseOrderQueryNoPharseInvHandler(
				this);
		ret.add(pharseOrderQueryNoPharseInvHandler);
		
		
		return ret;
	}

}
