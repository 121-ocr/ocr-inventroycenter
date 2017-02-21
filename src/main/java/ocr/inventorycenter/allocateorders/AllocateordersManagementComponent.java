package ocr.inventorycenter.allocateorders;

import java.util.ArrayList;
import java.util.List;

import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 仓库设施管理：货区管理
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class AllocateordersManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.ALLOCATEORDERSCOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_allocateorders";
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
		
		GetAllocateordersNameHandler getInvAreaNameHandler = new GetAllocateordersNameHandler(this);
		ret.add(getInvAreaNameHandler);
		
		AllocateordersQueryHandler queryHandler = new AllocateordersQueryHandler(this);
		ret.add(queryHandler);
		AllocateordersCreateHandler createHandler = new AllocateordersCreateHandler(this);
		ret.add(createHandler);
		AllocateordersUpdateHandler updateHandler = new AllocateordersUpdateHandler(this);
		ret.add(updateHandler);
		AllocateordersRemoveHandler removeHandler = new AllocateordersRemoveHandler(this);
		ret.add(removeHandler);

		return ret;
	}

}
