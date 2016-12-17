package ocr.inventorycenter;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.invfacility.InvFacilityManagementComponent;
import ocr.inventorycenter.invorg.InvOrgManagementComponent;
import ocr.inventorycenter.locationrelation.LocationRelationManagementComponent;
import ocr.inventorycenter.pharseinv.PharseInvManagementComponent;
import ocr.inventorycenter.sheftsrelation.SheftsRelationManagementComponent;
import ocr.inventorycenter.stockonhand.StockOnHandManagementComponent;
import ocr.inventorycenter.stockout.StockOutManagementComponent;
import ocr.inventorycenter.stockreserved.StockReservedManagementComponent;
import otocloud.framework.app.engine.AppServiceImpl;
import otocloud.framework.app.engine.WebServer;
import otocloud.framework.app.function.AppActivity;
import otocloud.framework.app.function.AppInitActivityImpl;

/**
 * 库存中心：
 * 
 * @date 2016年11月20日
 * @author LCL
 */
public class InventoryCenterService extends AppServiceImpl {

	// 创建服务初始化组件
	@Override
	public AppInitActivityImpl createAppInitActivity() {
		return null;
	}

	// 创建租户级web server
	@Override
	public WebServer createWebServer() {

		return null;
	}

	// 创建服务内的业务活动组件
	@Override
	public List<AppActivity> createBizActivities() {
		List<AppActivity> retActivities = new ArrayList<>();

		PharseInvManagementComponent pharseInvCom = new PharseInvManagementComponent();
		retActivities.add(pharseInvCom);

		InvOrgManagementComponent invOrgManagementComponent = new InvOrgManagementComponent();
		retActivities.add(invOrgManagementComponent);

		InvFacilityManagementComponent invFacilityManagementComponent = new InvFacilityManagementComponent();
		retActivities.add(invFacilityManagementComponent);

		LocationRelationManagementComponent locationRelationManagementComponent = new LocationRelationManagementComponent();
		retActivities.add(locationRelationManagementComponent);

		StockOnHandManagementComponent stockOnHand = new StockOnHandManagementComponent();
		retActivities.add(stockOnHand);

		StockReservedManagementComponent stockReserved = new StockReservedManagementComponent();
		retActivities.add(stockReserved);

		StockOutManagementComponent stockOut = new StockOutManagementComponent();
		retActivities.add(stockOut);

		SheftsRelationManagementComponent sheftsRelationManagementComponent = new SheftsRelationManagementComponent();
		retActivities.add(sheftsRelationManagementComponent);

		return retActivities;
	}
}
