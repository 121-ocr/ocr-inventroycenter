package ocr.inventorycenter.invorg;

import java.util.ArrayList;

import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;
/**
   库存组织规划：对象（仓库档案）
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class InvOrgManagementComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		return "invorg-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_warehouses";
	}

	//发布此业务活动关联的业务角色
	@Override
	public List<BizRoleDescriptor> exposeBizRolesDesc() {
		return null;
	}

	//发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		return null;
	}


	//业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {
		
		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();
		
		InvOrgQueryHandler queryHandler = new InvOrgQueryHandler(this);
		ret.add(queryHandler);
		
		GetWarehouseNameHandler getWarehouseNameHandler = new GetWarehouseNameHandler(this);
		ret.add(getWarehouseNameHandler);
		
		return ret;
	}

}
