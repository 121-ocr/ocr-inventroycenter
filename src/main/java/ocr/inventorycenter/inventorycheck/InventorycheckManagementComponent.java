package ocr.inventorycenter.inventorycheck;

import java.util.ArrayList;
import java.util.List;

import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 库存盘点
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class InventorycheckManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.INVENTORYCHECKCOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_inventorycheck";
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
		
		GetInventorycheckNameHandler getInvAreaNameHandler = new GetInventorycheckNameHandler(this);
		ret.add(getInvAreaNameHandler);
		
		InventorycheckQueryHandler queryHandler = new InventorycheckQueryHandler(this);
		ret.add(queryHandler);
		InventorycheckCreateHandler createHandler = new InventorycheckCreateHandler(this);
		ret.add(createHandler);
		InventorycheckUpdateHandler updateHandler = new InventorycheckUpdateHandler(this);
		ret.add(updateHandler);
		InventorycheckRemoveHandler removeHandler = new InventorycheckRemoveHandler(this);
		ret.add(removeHandler);
		InventorycheckConfirmHandler confirmHandler = new InventorycheckConfirmHandler(this);
		ret.add(confirmHandler);

		return ret;
	}

}
