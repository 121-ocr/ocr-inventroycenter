package ocr.inventorycenter.stockreserved;

import java.util.ArrayList;
import java.util.List;

import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;
/**
 * 库存中心：预留-管理
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class StockReservedManagementComponent extends AppActivityImpl {

	//业务活动组件名
	@Override
	public String getName() {
		return "stockreserverd";
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "bc_stockreserverd";
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

		StockReservedCreationHandler initHandler = new StockReservedCreationHandler(this);
		ret.add(initHandler);
		
		
		StockReservedQueryHandler queryHandler = new StockReservedQueryHandler(this);
		ret.add(queryHandler);

		StockReservedRemoveHandler removeHandler = new StockReservedRemoveHandler(this);
		ret.add(removeHandler);
		
		StockReservedNumQueryHandler srNumHandler = new StockReservedNumQueryHandler(this);
		ret.add(srNumHandler);
		
		StockReservedSaveHandler saveHandler = new StockReservedSaveHandler(this);
		ret.add(saveHandler);
		
		return ret;
	}

}
