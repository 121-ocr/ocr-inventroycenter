package ocr.inventorycenter.shelfwarning;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.stockonhand.StockOnHandConstant;
import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 保质期预警组件
 * @author pcitc
 *
 */
public class ShelfWarningComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.SHELFWARNINGCOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return StockOnHandConstant.ComponentBizObjectTypeConstant;
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
		
		ShelfWarningQueryHandler shelfWarningQueryHandler = new ShelfWarningQueryHandler(this);
		ret.add(shelfWarningQueryHandler);
		
		return ret;
	}

}
