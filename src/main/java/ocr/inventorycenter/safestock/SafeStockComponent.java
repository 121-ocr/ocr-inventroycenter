package ocr.inventorycenter.safestock;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
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

	// 发布此业务活动关联的业务角色
	@Override
	public List<BizRoleDescriptor> exposeBizRolesDesc() {
		return null;
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
		
		return ret;
	}

}
