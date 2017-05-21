package ocr.inventorycenter.invorg;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 库存组织
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class InvOrgComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return "invorg-mgr";
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_invorg";
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

		InvOrgCreateHandler createHandler = new InvOrgCreateHandler(this);
		ret.add(createHandler);
		
		InvOrgQueryHandler invOrgQueryAllHandler = new InvOrgQueryHandler(this);
		ret.add(invOrgQueryAllHandler);


		return ret;
	}

}
