package ocr.inventorycenter;

import java.util.ArrayList;
import java.util.List;

import ocr.inventorycenter.allocateorders.AllocateordersManagementComponent;
import ocr.inventorycenter.invarea.InvAreaManagementComponent;
import ocr.inventorycenter.inventorycheck.InventorycheckManagementComponent;
import ocr.inventorycenter.invfacility.InvFacilityManagementComponent;
import ocr.inventorycenter.invorg.InvOrgComponent;
import ocr.inventorycenter.locationrelation.LocationRelationManagementComponent;
import ocr.inventorycenter.pharseinv.PharseInvManagementComponent;
import ocr.inventorycenter.pharseorder.PharseOrderManagementComponent;
import ocr.inventorycenter.safestock.SafeStockComponent;
import ocr.inventorycenter.safestockwarning.SafeStockWarningComponent;
import ocr.inventorycenter.sheftsrelation.SheftsRelationManagementComponent;
import ocr.inventorycenter.shelfwarning.ShelfWarningComponent;
import ocr.inventorycenter.stockonhand.StockOnHandManagementComponent;
import ocr.inventorycenter.stockout.StockOutManagementComponent;
import ocr.inventorycenter.stockreserved.StockReservedManagementComponent;
import ocr.inventorycenter.suppliers.SuppliersManagementComponent;
import ocr.inventorycenter.unit.InvUnitManagementComponent;
import ocr.inventorycenter.warehouse.WarehouseComponent;
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

		WarehouseComponent invOrgManagementComponent = new WarehouseComponent();
		retActivities.add(invOrgManagementComponent);

		InvFacilityManagementComponent invFacilityManagementComponent = new InvFacilityManagementComponent();
		retActivities.add(invFacilityManagementComponent);

		InvAreaManagementComponent InvAreaManagementComponent = new InvAreaManagementComponent();
		retActivities.add(InvAreaManagementComponent);

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

		InvUnitManagementComponent invUnitManagementComponent = new InvUnitManagementComponent();
		retActivities.add(invUnitManagementComponent);

		PharseOrderManagementComponent pharseOrderManagementComponent = new PharseOrderManagementComponent();
		retActivities.add(pharseOrderManagementComponent);
		
		SuppliersManagementComponent suppliersManagementComponet = new SuppliersManagementComponent();
		retActivities.add(suppliersManagementComponet);
		
		AllocateordersManagementComponent allocateordersManagementComponent = new AllocateordersManagementComponent();
		retActivities.add(allocateordersManagementComponent);
		
		InventorycheckManagementComponent invcheckManagementComponet = new InventorycheckManagementComponent();
		retActivities.add(invcheckManagementComponet);
		
		ShelfWarningComponent shelfWarningComponent = new ShelfWarningComponent();
		retActivities.add(shelfWarningComponent);
		
		SafeStockComponent safeStockComponent = new SafeStockComponent();
		retActivities.add(safeStockComponent);
		
		SafeStockWarningComponent safeStockWarningComponent = new SafeStockWarningComponent();
		retActivities.add(safeStockWarningComponent);
		
		InvOrgComponent invOrgComponent = new InvOrgComponent();
		retActivities.add(invOrgComponent);
				

		return retActivities;
	}
}
