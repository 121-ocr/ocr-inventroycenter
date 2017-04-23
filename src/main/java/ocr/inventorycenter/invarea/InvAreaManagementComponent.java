package ocr.inventorycenter.invarea;

import java.util.ArrayList;
import java.util.List;

import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 仓库设施管理：货区管理
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class InvAreaManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.AREACOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_invarea";
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
		
		GetInvAreaNameHandler getInvAreaNameHandler = new GetInvAreaNameHandler(this);
		ret.add(getInvAreaNameHandler);
		
		InvAreaQueryHandler queryHandler = new InvAreaQueryHandler(this);
		ret.add(queryHandler);
		InvAreaCreateHandler createHandler = new InvAreaCreateHandler(this);
		ret.add(createHandler);
		InvAreaUpdateHandler updateHandler = new InvAreaUpdateHandler(this);
		ret.add(updateHandler);
		InvAreaRemoveHandler removeHandler = new InvAreaRemoveHandler(this);
		ret.add(removeHandler);

		return ret;
	}

}
