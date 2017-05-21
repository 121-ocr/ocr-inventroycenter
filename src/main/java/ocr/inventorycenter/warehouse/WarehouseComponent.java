package ocr.inventorycenter.warehouse;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 库存组织规划：（仓库档案）
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class WarehouseComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return "warehouse-mgr";
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_warehouses";
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

		WarehouseQueryAllHandler queryHandler = new WarehouseQueryAllHandler(this);
		ret.add(queryHandler);
		
		GetWarehouseNameHandler getWarehouseNameHandler = new GetWarehouseNameHandler(this);
		ret.add(getWarehouseNameHandler);

		WarehouseCreateHandler createHandler = new WarehouseCreateHandler(this);
		ret.add(createHandler);
		
		WarehouseQueryHandler queryHandler2 = new WarehouseQueryHandler(this);
		ret.add(queryHandler2);
		
		WarehouseQueryNoPagingHandler warehouseQueryNoPagingHandler = new WarehouseQueryNoPagingHandler(this);
		ret.add(warehouseQueryNoPagingHandler);
		
		WarehouseUpdateHandler updateHandler = new WarehouseUpdateHandler(this);
		ret.add(updateHandler);
		WarehouseRemoveHandler removeHandler = new WarehouseRemoveHandler(this);
		ret.add(removeHandler);
		
		WarehouseOwnerBizUnitGetHandler warehouseOwnerBizUnitGetHandler = new WarehouseOwnerBizUnitGetHandler(this);
		ret.add(warehouseOwnerBizUnitGetHandler);

		return ret;
	}

}
