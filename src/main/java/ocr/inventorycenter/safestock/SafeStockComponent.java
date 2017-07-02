package ocr.inventorycenter.safestock;

import java.util.ArrayList;
import java.util.List;

import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 安全库存组件
 * @author pcitc
 *
 */
public class SafeStockComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.SAFESTOCK;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return SafeStockConstant.ComponentBizObjectTypeConstant;
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
		
		SafeStockCreationHandler safeStockCreationHandler = new SafeStockCreationHandler(this);
		ret.add(safeStockCreationHandler);
		SafeStockUpdateHandler safeStockUpdateHandler = new SafeStockUpdateHandler(this);
		ret.add(safeStockUpdateHandler);
		SafeStockRemoveHandler safeStockRemoveHandler = new SafeStockRemoveHandler(this);
		ret.add(safeStockRemoveHandler);
		SafeStockQueryHandler safeStockQueryHandler = new SafeStockQueryHandler(this);
		ret.add(safeStockQueryHandler);
		SafeStockQuery4WarningHandler safeStockQuery4WarningHandler = new SafeStockQuery4WarningHandler(this);
		ret.add(safeStockQuery4WarningHandler);
		
		return ret;
	}

}