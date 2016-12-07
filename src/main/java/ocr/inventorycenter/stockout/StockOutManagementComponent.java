package ocr.inventorycenter.stockout;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.stockreserved.StockReservedNumQueryHandler;
import ocr.inventorycenter.stockreserved.StockReservedRemoveHandler;
import ocr.inventorycenter.stockreserved.StockReservedSaveHandler;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.app.function.BizRoleDescriptor;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 拣货出库单
 * 
 * @author LCL
 *
 */
public class StockOutManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {

		return StockOutConstant.ComponentNameConstant;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return StockOutConstant.ComponentBizObjectTypeConstant;
	}

	// 发布此业务活动关联的业务角色
	@Override
	public List<BizRoleDescriptor> exposeBizRolesDesc() {
		// TODO Auto-generated method stub
		BizRoleDescriptor bizRole = new BizRoleDescriptor("2", "核心企业");

		List<BizRoleDescriptor> ret = new ArrayList<BizRoleDescriptor>();
		ret.add(bizRole);
		return ret;
	}

	// 发布此业务活动对外暴露的业务事件
	@Override
	public List<OtoCloudEventDescriptor> exposeOutboundBizEventsDesc() {
		// TODO Auto-generated method stub
		return null;
	}

	// 业务活动组件中的业务功能
	@Override
	public List<OtoCloudEventHandlerRegistry> registerEventHandlers() {

		List<OtoCloudEventHandlerRegistry> ret = new ArrayList<OtoCloudEventHandlerRegistry>();

		StockOutCreateHandler createHandler = new StockOutCreateHandler(this);
		ret.add(createHandler);

		StockOutQueryHandler queryHandler = new StockOutQueryHandler(this);
		ret.add(queryHandler);

		StockOutRemoveHandler removeHandler = new StockOutRemoveHandler(this);
		ret.add(removeHandler);

		StockOutPickOutHandler pickoutHandler = new StockOutPickOutHandler(this);
		ret.add(pickoutHandler);

		StockOutShippingHandler shippingHandler = new StockOutShippingHandler(this);
		ret.add(shippingHandler);
		
		StockOutBatchPickOutHandler pickoutbatchHandler = new StockOutBatchPickOutHandler(this);
		ret.add(pickoutbatchHandler);

		StockOutBatchPickOutTESTHandler testHandler = new StockOutBatchPickOutTESTHandler(this);
		ret.add(testHandler);
		
		StockOutOnShippingHandler onshippingHandler = new StockOutOnShippingHandler(this);
		ret.add(onshippingHandler);
		
		
	
		
		return ret;
	}

}
