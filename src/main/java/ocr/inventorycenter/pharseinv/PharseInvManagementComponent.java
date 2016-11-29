package ocr.inventorycenter.pharseinv;

import java.util.ArrayList;

import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;
/**
 * 库存中心：采购入库
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class PharseInvManagementComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		return "pharseinv-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "bp_pharseinv";
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

		PharseInvCreationHandler initHandler = new PharseInvCreationHandler(this);
		ret.add(initHandler);
		
		
		PharseInvQueryHandler queryHandler = new PharseInvQueryHandler(this);
		ret.add(queryHandler);

		PharseInvRemoveHandler removeHandler = new PharseInvRemoveHandler(this);
		ret.add(removeHandler);
		
		return ret;
	}

}
