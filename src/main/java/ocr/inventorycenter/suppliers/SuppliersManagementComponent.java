package ocr.inventorycenter.suppliers;

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
public class SuppliersManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.SUPPLIERSCOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "ba_suppliers";
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
		
		GetSuppliersNameHandler getInvAreaNameHandler = new GetSuppliersNameHandler(this);
		ret.add(getInvAreaNameHandler);
		
		SuppliersQueryHandler queryHandler = new SuppliersQueryHandler(this);
		ret.add(queryHandler);
		SuppliersCreateHandler createHandler = new SuppliersCreateHandler(this);
		ret.add(createHandler);
		SuppliersUpdateHandler updateHandler = new SuppliersUpdateHandler(this);
		ret.add(updateHandler);
		SuppliersRemoveHandler removeHandler = new SuppliersRemoveHandler(this);
		ret.add(removeHandler);
		
		SuppliersQueryNoPagingHandler suppliersQueryNoPagingHandler = new SuppliersQueryNoPagingHandler(this);
		ret.add(suppliersQueryNoPagingHandler);

		return ret;
	}

}
