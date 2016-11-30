package ocr.inventorycenter.stockonhand;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;
/**
 * 库存中心：现存量-管理@
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class StockOnHandManagementComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		return "stockonhand-mgr";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "bc_stockonhand";
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

		StockOnHandCreationHandler createHandler = new StockOnHandCreationHandler(this);
		ret.add(createHandler);
		
		StockOnHandQueryHandler queryHandler = new StockOnHandQueryHandler(this);
		ret.add(queryHandler);

		StockOnHandRemoveHandler removeHandler = new StockOnHandRemoveHandler(this);
		ret.add(removeHandler);
		
		StockOnHandUpdateHandler updateHandler = new StockOnHandUpdateHandler(this);
		ret.add(updateHandler);
		
		return ret;
	}

}
