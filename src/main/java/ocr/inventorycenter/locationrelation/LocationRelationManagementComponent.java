package ocr.inventorycenter.locationrelation;

import java.util.ArrayList;
import java.util.List;

import orc.common.busi.open.inventorycenter.InvBusiOpenContant;
import otocloud.framework.app.function.AppActivityImpl;
import otocloud.framework.core.OtoCloudEventDescriptor;
import otocloud.framework.core.OtoCloudEventHandlerRegistry;

/**
 * 货位商品关系档案
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class LocationRelationManagementComponent extends AppActivityImpl {

	// 业务活动组件名
	@Override
	public String getName() {
		return InvBusiOpenContant.LOCATIONRELATIONCOMPONTENNAME;
	}

	// 业务活动组件要处理的核心业务对象
	@Override
	public String getBizObjectType() {
		return "bs_locationrelation";
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

		GetNameLocationsGoodsRelationQueryHandler queryHandler = new GetNameLocationsGoodsRelationQueryHandler(this);
		ret.add(queryHandler);

		LocationsGoodsRelationCreateHandler locationsGoodsRelationCreateHandler = new LocationsGoodsRelationCreateHandler(
				this);
		ret.add(locationsGoodsRelationCreateHandler);

		LocationsGoodsRelationQueryHandler locationsGoodsRelationQueryHandler = new LocationsGoodsRelationQueryHandler(
				this);
		ret.add(locationsGoodsRelationQueryHandler);

		LocationsGoodsRelationRemoveHandler locationsGoodsRelationRemoveHandler = new LocationsGoodsRelationRemoveHandler(
				this);
		ret.add(locationsGoodsRelationRemoveHandler);

		LocationsGoodsRelationUpdateHandler locationsGoodsRelationUpdateHandler = new LocationsGoodsRelationUpdateHandler(
				this);
		ret.add(locationsGoodsRelationUpdateHandler);

		return ret;
	}

}
