package ocr.inventorycenter.stockonhand;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.stockonhand.sales.StockOnHandQuery4SalesHandler;
import otocloud.framework.app.function.AppActivityImpl;
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
		return StockOnHandConstant.ComponentNameConstant;
	}
	
	//业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return StockOnHandConstant.ComponentBizObjectTypeConstant;
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
		
		StockOnHandUpdateStatusHandler stockOnHandUpdateStatusHandler = new StockOnHandUpdateStatusHandler(this);
		ret.add(stockOnHandUpdateStatusHandler);
		
		StockOnHandQuery4SalesHandler query4SalesHandler = new StockOnHandQuery4SalesHandler(this);
		ret.add(query4SalesHandler);
		
		StockOnHandBatchCreationHandler stockOnHandBatchCreationHandler = new StockOnHandBatchCreationHandler(this);
		ret.add(stockOnHandBatchCreationHandler);
		
		MatchLocationByInvBatchHandler matchLocationByInvBatchHandler = new MatchLocationByInvBatchHandler(this);
		ret.add(matchLocationByInvBatchHandler);
		
		StockOnHandQueryByFIFOHandler stockOnHandQueryByFIFOHandler = new StockOnHandQueryByFIFOHandler(this);
		ret.add(stockOnHandQueryByFIFOHandler);
		
		StockOnHandQueryBySkuHandler stockOnHandQueryBySkuHandler = new StockOnHandQueryBySkuHandler(this);
		ret.add(stockOnHandQueryBySkuHandler);
		
		AutoMarchLocationByOnHandHandler autoMarchLocationByOnHandHandler = new AutoMarchLocationByOnHandHandler(this);
		ret.add(autoMarchLocationByOnHandHandler);
		
		StockOnHandQuery4ShelfWarningHandler stockOnHandQuery4ShelfWarningHandler = new StockOnHandQuery4ShelfWarningHandler(this);
		ret.add(stockOnHandQuery4ShelfWarningHandler);
		
		StockOnHandQuery4SafeStockWarningHandler stockOnHandQuery4SafeStockWarningHandler = new StockOnHandQuery4SafeStockWarningHandler(this);
		ret.add(stockOnHandQuery4SafeStockWarningHandler);
		
		
		return ret;
	}

}
