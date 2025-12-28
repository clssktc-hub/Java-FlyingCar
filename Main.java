import java.util.concurrent.TimeUnit;

// --- 介面 1 ---
interface Drivable {
    void drive();
    void stopDriving();
}

// --- 介面 2 ---
interface Flyable {
    void takeOff();
    void fly();
    void land();
}

// --- 抽象類別 (EV 功能) ---
abstract class Vehicle {
    private String model;
    private boolean systemOnline;
    protected double batteryCharge; // 單位 %
    protected int currentSpeed; // 單位 km/h

    public Vehicle(String model) {
        this.model = model;
        this.systemOnline = false;
        this.batteryCharge = 100.0;
        this.currentSpeed = 0;
    }

    public void powerOn() {
        if (!systemOnline) {
            this.systemOnline = true;
            consumeBattery(0.2);
            System.out.println(model + " 系統啟動。目前電量：" + String.format("%.1f", batteryCharge) + "%");
        }
    }

    public void powerOff() {
        if (systemOnline) {
            this.systemOnline = false;
            this.currentSpeed = 0;
            System.out.println(model + " 系統關閉。");
        } else {
            System.out.println(model + " 系統已被 (EPS) 緊急切斷。");
        }
    }

    protected void emergencyPowerCut() { this.systemOnline = false; }

    public void charge() {
        System.out.println(model + " 正在連接超級充電站...");
        this.batteryCharge = 100.0;
        System.out.println("充電完成！目前電量：100%");
    }

    public void engageAutopilot() {
        if (systemOnline && currentSpeed > 0) {
            System.out.println(model + " 啟動 Autopilot 自動輔助駕駛。");
        } else {
            System.out.println(model + " 無法啟動 Autopilot (系統未啟動或車輛未行駛)。");
        }
    }

    // 基礎的消耗/回充方法，供 EMS 呼叫
    protected boolean consumeBattery(double amount) {
        if (this.batteryCharge >= amount) {
            this.batteryCharge -= amount;
            return true;
        } else {
            System.out.println(model + " [檢查失敗] 電力不足！(需要 " + amount + "%, 僅剩 " + String.format("%.1f", batteryCharge) + "%)");
            return false;
        }
    }

    protected void regainBattery(double amount) {
        this.batteryCharge += amount;
        if (this.batteryCharge > 100.0) this.batteryCharge = 100.0;
        System.out.println(model + " [動能回收] 啟動。電力恢復 " + amount + "%。 (目前: " + String.format("%.1f", batteryCharge) + "%)");
    }

    // --- Getters (給輔助系統使用) ---
    public String getModel() { return model; }
    public boolean isSystemOnline() { return systemOnline; }
    public double getBatteryCharge() { return batteryCharge; }
    public int getCurrentSpeed() { return currentSpeed; }
}


// --- 實體類別 1：會飛的汽車 (已整合 EMS, EPS, SOP) ---
class FlyingCar extends Vehicle implements Drivable, Flyable {

    // 狀態機
    private enum OperatingMode {
        GROUND, TRANSFORMING_TO_AIR, FLIGHT_READY, AIRBORNE, LANDED, TRANSFORMING_TO_GROUND,
        CRASHING
    }
    private OperatingMode currentMode;
    private boolean isLudicrousMode = false;

    // 輔助系統
    private FlightChecklist checklist;
    private EmergencyProtectionSystem eps;
    private EnergyManagementSystem ems;

    // --- 模擬的內部感測器狀態 ---
    private boolean isParkingBrakeOn = true;
    private double groundTilt = 3.0;
    private boolean isInVertiport = true;
    private boolean isObstacleNear = false;
    private int visibility = 3;
    private int windSpeed = 5;
    private boolean isWingLockSensorOk = true;
    private boolean isPropellerClear = true;
    private boolean isCabinDoorClosed = true;
    private boolean isBMSOk = true;
    private double currentWeight = 350.0;
    private double maxTakeoffWeight = 400.0;
    private boolean isPassengerBelted = true;
    private int currentAltitude = 0;
    private boolean isPropulsionOk = true;
    private boolean isStructuralOk = true;
    private boolean isFlightControlOk = true;
    private boolean isIMUHealthy = true;
    private int gnssSatellites = 9;
    private boolean isBarometerOk = true;

    // --- Getters for Checklist, EPS, EMS ---
    public boolean isParkingBrakeOn() { return isParkingBrakeOn; }
    public double getGroundTilt() { return groundTilt; }
    public boolean isInVertiport() { return isInVertiport; }
    public boolean isObstacleNear() { return isObstacleNear; }
    public int getVisibility() { return visibility; }
    public int getWindSpeed() { return windSpeed; }
    public boolean isWingLockSensorOk() { return isWingLockSensorOk; }
    public boolean isPropellerClear() { return isPropellerClear; }
    public boolean isCabinDoorClosed() { return isCabinDoorClosed; }
    public boolean isBMSOk() { return isBMSOk; }
    public boolean isIMUHealthy() { return isIMUHealthy; }
    public int getGnssSatellites() { return gnssSatellites; }
    public boolean isBarometerOk() { return isBarometerOk; }
    public double getCurrentWeight() { return currentWeight; }
    public double getMaxTakeoffWeight() { return maxTakeoffWeight; }
    public boolean isPassengerBelted() { return isPassengerBelted; }
    public int getCurrentAltitude() { return currentAltitude; }
    public boolean isPropulsionOk() { return isPropulsionOk; }
    public boolean isStructuralOk() { return isStructuralOk; }
    public boolean isFlightControlOk() { return isFlightControlOk; }
    public EnergyManagementSystem getEMS() { return ems; }

    public boolean isPropellerStopped() {
        return currentMode == OperatingMode.GROUND || currentMode == OperatingMode.FLIGHT_READY;
    }

    // 建構子
    public FlyingCar(String model) {
        super(model);
        this.currentMode = OperatingMode.GROUND;
        this.checklist = new FlightChecklist(this);
        this.eps = new EmergencyProtectionSystem(this);
        this.ems = new EnergyManagementSystem(this);
    }

    private boolean isCrashed() {
        if (currentMode == OperatingMode.CRASHING) {
            System.out.println("[系統鎖定] 車輛處於 CRASHING 狀態，指令無效。");
            return true;
        }
        return false;
    }

    // --- 1. 語音指令：「我要飛行模式」 ---
    public void requestFlightMode() throws InterruptedException {
        if (isCrashed()) return;
        System.out.println("\n[指令] \"我要飛行模式\"");

        if (!this.checklist.runPreTakeoffChecklist()) {
            System.out.println("[系統] 起飛前檢查未通過。無法轉換模式。");
            return;
        }

        System.out.println("[系統] 正在轉換為飛行模式...");
        this.currentMode = OperatingMode.TRANSFORMING_TO_AIR;

        if (executeAirTransformSOP()) {
            this.currentMode = OperatingMode.FLIGHT_READY;
            System.out.println("[系統] 飛行模式已準備完畢，可隨時起飛。");
        } else {
            this.currentMode = OperatingMode.GROUND;
        }
    }

    // --- 2. 語音指令：「起飛」 ---
    public void requestTakeOff() {
        if (isCrashed()) return;
        System.out.println("\n[指令] \"起飛\"");

        if (this.currentMode != OperatingMode.FLIGHT_READY && this.currentMode != OperatingMode.LANDED) {
            System.out.println("[系統] 起飛失敗：必須處於 [飛行準備就緒] 或 [已著陸] 狀態。");
            return;
        }

        if (!runTakeOffPreChecks()) return;

        System.out.println("[系統] 正在起飛...");
        executeTakeOff();
        this.currentMode = OperatingMode.AIRBORNE;
    }

    // --- 3. 語音指令：巡航 ---
    public void requestFly() throws InterruptedException {
        if (isCrashed()) return;
        if (this.currentMode != OperatingMode.AIRBORNE) {
            System.out.println("[系統] 巡航失敗：尚未起飛。");
            return;
        }

        if (eps.checkForFatalErrors()) {
            this.currentMode = OperatingMode.CRASHING;
            eps.activatePreCrashSequence();
            return;
        }

        double consumption = ems.getCruiseConsumption();
        System.out.println("[系統] " + (ems.isEcoMode() ? "E-Mode" : "標準") + " 巡航... (預計消耗 " + consumption + "%)");

        if (consumeBattery(consumption)) {
            this.currentSpeed = 200;
            System.out.println("[系統] 正在 " + this.currentAltitude + " 公尺高空巡航，時速 " + currentSpeed + " km/h。");
        } else {
            System.out.println("[系統] 飛行失敗：電力不足。");
            requestLanding();
        }

        if (ems.isBelowLandingReserve()) {
            System.out.println("[EMS 警告] 電量已低於 " + EnergyManagementSystem.LANDING_RESERVE_SOC + "%。觸發自動降落！");
            requestLanding();
        }
    }

    // --- 4. 語音指令：降落 ---
    public void requestLanding() {
        if (isCrashed() || this.currentMode == OperatingMode.LANDED) return;
        System.out.println("\n[指令] \"我要降落\"");

        if (this.currentMode != OperatingMode.AIRBORNE) {
            System.out.println("[系統] 降落失敗：尚未起飛。");
            return;
        }

        if (!this.checklist.runPreLandingChecklist()) {
            System.out.println("[系統] 降落前檢查未通過。已中斷自動降落程序。");
            return;
        }

        System.out.println("[系統] 收到指令... 開始自動降落程序...");
        executeLand();
        this.currentMode = OperatingMode.LANDED;
        System.out.println("[系統] 已著陸 (Weight-on-Wheels)。可切換回地面模式。");
    }

    // --- 5. 語音指令：恢復汽車模式 ---
    public void requestGroundMode() throws InterruptedException {
        if (isCrashed()) return;
        System.out.println("\n[指令] \"恢復成汽車模式\"");

        if (this.currentMode != OperatingMode.LANDED && this.currentMode != OperatingMode.FLIGHT_READY) {
            System.out.println("[系統] 轉換失敗：必須處於 [已著陸] 或 [飛行準備就緒] 狀態。");
            return;
        }

        if (!this.checklist.runPostLandingChecklist()) {
            System.out.println("[系統] 著陸後檢查未通過。無法轉換模式。");
            return;
        }

        System.out.println("[系統] 正在切換回地面模式...");
        this.currentMode = OperatingMode.TRANSFORMING_TO_GROUND;

        if (executeGroundTransformSOP()) {
            this.currentMode = OperatingMode.GROUND;
            System.out.println("[系統] 地面模式已啟用，可以行駛。");
        }
    }

    // --- 內部SOP (簡化) ---
    private boolean executeAirTransformSOP() throws InterruptedException {
        simulateProcess("SOP 1. 鎖定輪子", 50);
        simulateProcess("SOP 2. 展開主翼並雙重鎖定", 100);
        return true;
    }
    private boolean executeGroundTransformSOP() throws InterruptedException {
        simulateProcess("SOP 1. 收回主翼並鎖定", 100);
        simulateProcess("SOP 2. 尾翼折疊", 50);
        return true;
    }
    private boolean runTakeOffPreChecks() {
        System.out.println("[系統] 執行起飛前最終檢查...");
        if (!consumeBattery(20.0)) { // 起飛消耗 20%
            return false;
        }
        System.out.println("...[檢查通過] 風況穩定、無障礙物。");
        return true;
    }
    private void executeTakeOff() {
        this.currentAltitude = 150;
        this.currentSpeed = 50;
        System.out.println("[系統] 垂直起飛！正在爬升至 " + this.currentAltitude + "m... 電量剩餘：" + String.format("%.1f", batteryCharge) + "%");
    }
    private void executeLand() {
        this.currentAltitude = 0;
        this.currentSpeed = 0;
        System.out.println("[系統] 準備降落... 高度 20m... 10m... 著陸。");
    }
    private void simulateProcess(String message, int milliseconds) throws InterruptedException {
        System.out.println(message);
        Thread.sleep(milliseconds);
    }

    // --- 汽車駕駛功能 ---
    @Override
    public void drive() {
        if (isCrashed()) return;
        if (currentMode != OperatingMode.GROUND) {
            System.out.println("[系統] 駕駛失敗：目前模式 (" + currentMode + ") 不允許駕駛。");
            return;
        }
        if (!isSystemOnline()) {
            System.out.println(getModel() + " 駕駛失敗：請先啟動系統。");
            return;
        }
        this.currentSpeed = isLudicrousMode ? 210 : 120;
        System.out.println("[系統] " + (isLudicrousMode ? "Plaid模式" : "標準模式") + " 正在地面行駛。");
        consumeBattery(isLudicrousMode ? 3.0 : 1.0); // 地面駕駛耗電
    }
    @Override
    public void stopDriving() {
        if (isCrashed()) return;
        if (currentMode != OperatingMode.GROUND || this.currentSpeed == 0) return;
        this.currentSpeed = 0;
        ems.activateRegenerativeBraking();
        System.out.println("[系統] 停止地面行駛。");
    }
    public void toggleLudicrousMode() {
        if (isCrashed()) return;
        if (currentMode != OperatingMode.GROUND) {
            System.out.println("[系統] 模式切換失敗：僅 [地面模式] 可用。");
            return;
        }
        this.isLudicrousMode = !this.isLudicrousMode;
        System.out.println("[系統] Plaid 模式 " + (isLudicrousMode ? "啟動" : "關閉") + "!");
    }

    // --- 新增：EMS 指令 ---
    public void toggleEcoMode() {
        if (isCrashed()) return;
        ems.toggleEcoMode();
    }

    // --- 測試用：模擬系統故障 ---
    public void simulateSystemFailure(String failureType) {
        System.out.println("[主控] 注入故障：" + failureType);
        switch (failureType) {
            case "Propulsion": this.isPropulsionOk = false; break;
            case "Structure": this.isStructuralOk = false; break;
            case "Control": this.isFlightControlOk = false; break;
            case "Battery": this.batteryCharge = 5.0; break;
        }
    }

    // 測試用：模擬低電量
    public void simulateLowBattery(double soc) {
        this.batteryCharge = soc;
    }

    @Override public void takeOff() { System.out.println("[API 提示] 請改用 'requestTakeOff()'。"); }
    @Override public void fly() { System.out.println("[API 提示] 請改用 'requestFly()'。"); }
    @Override public void land() { System.out.println("[API 提示] 請改用 'requestLanding()'。"); }
}


// --- 輔助類別 1：EnergyManagementSystem ---
class EnergyManagementSystem {
    private FlyingCar car;

    public static final double TAKEOFF_MIN_SOC = 60.0;
    public static final double LANDING_RESERVE_SOC = 25.0;
    public static final double MAX_RANGE_KM = 300.0;

    private boolean isEcoMode = false;
    private double plannedRangeKM = 150.0;

    private static final double NORMAL_CRUISE_CONSUMPTION = 12.0;
    private static final double ECO_CRUISE_CONSUMPTION = 8.0;
    private static final double REGEN_BRAKING_GAIN = 2.5;

    public EnergyManagementSystem(FlyingCar car) {
        this.car = car;
    }

    public boolean runPreflightCheck() {
        double currentSOC = car.getBatteryCharge();

        if (currentSOC < TAKEOFF_MIN_SOC) {
            return fail("電量低於 " + TAKEOFF_MIN_SOC + "% (目前 " + String.format("%.1f", currentSOC) + "%)");
        }

        double estimatedConsumption = calculateEstimatedConsumption(this.plannedRangeKM);
        if (estimatedConsumption > currentSOC) {
            return fail("航程預估消耗 (" + String.format("%.1f", estimatedConsumption) + "%) > 目前電量 (" + String.format("%.1f", currentSOC) + "%)");
        }

        return pass("電量充足 (SOC " + String.format("%.1f", currentSOC) + "%)，預估航程消耗 " + String.format("%.1f", estimatedConsumption) + "%");
    }

    private double calculateEstimatedConsumption(double rangeKM) {
        return (rangeKM / MAX_RANGE_KM) * (100.0 - LANDING_RESERVE_SOC) + LANDING_RESERVE_SOC;
    }

    public double getCruiseConsumption() {
        return isEcoMode ? ECO_CRUISE_CONSUMPTION : NORMAL_CRUISE_CONSUMPTION;
    }

    public boolean checkLandingReserve() {
        double currentSOC = car.getBatteryCharge();
        if (currentSOC < LANDING_RESERVE_SOC) {
            return fail("電量低於安全備援 " + LANDING_RESERVE_SOC + "% (目前 " + String.format("%.1f", currentSOC) + "%)");
        }
        return pass("電量 > " + LANDING_RESERVE_SOC + "% (剩餘 " + String.format("%.1f", currentSOC) + "%)");
    }

    public boolean isBelowLandingReserve() {
        return car.getBatteryCharge() < LANDING_RESERVE_SOC;
    }

    public void activateRegenerativeBraking() {
        car.regainBattery(REGEN_BRAKING_GAIN);
    }

    public void toggleEcoMode() {
        this.isEcoMode = !this.isEcoMode;
        System.out.println("[EMS] E-Mode (節能巡航) " + (isEcoMode ? "啟動" : "關閉"));
    }
    public boolean isEcoMode() { return isEcoMode; }

    private boolean pass(String message) { System.out.println("   [通過] " + message); return true; }
    private boolean fail(String message) { System.out.println("   [失敗] " + message); return false; }
}


// --- 輔助類別 2：FlightChecklist ---
class FlightChecklist {
    private FlyingCar car;
    public FlightChecklist(FlyingCar car) { this.car = car; }

    public boolean runPreTakeoffChecklist() {
        System.out.println("--- 執行起飛前安全檢查 (Pre-Takeoff Checklist) ---");
        return checkA_Environment() &&
                checkB_Structure() &&
                checkC_Power() &&
                checkD_Sensors() &&
                checkE_WeightAndBalance() &&
                checkF_Cabin();
    }
    public boolean runPreLandingChecklist() {
        System.out.println("--- 執行降落前安全檢查 (Pre-Landing Checklist) ---");
        return checkL_A_GroundEnv() &&
                checkL_B_Power() &&
                checkL_C_Sensors() &&
                checkL_D_Structure();
    }
    public boolean runPostLandingChecklist() {
        System.out.println("--- 執行著陸後切換檢查 ---");
        if (car.getCurrentSpeed() > 10) return fail("地面速度 > 10 km/h");
        if (!car.isPropellerStopped()) return fail("推進器尚未完全停止");
        if (car.isObstacleNear()) return fail("周遭有障礙物，禁止收翼");
        return pass("可切換為地面模式");
    }

    private boolean pass(String message) { System.out.println("   [通過] " + message); return true; }
    private boolean fail(String message) { System.out.println("   [失敗] " + message); return false; }

    private boolean checkA_Environment() {
        System.out.println("A. 車輛靜止與環境檢查...");
        if (car.getCurrentSpeed() != 0) return fail("車速不為 0");
        if (!car.isParkingBrakeOn()) return fail("駐車煞車未啟動");
        return pass("環境檢查通過");
    }
    private boolean checkB_Structure() {
        System.out.println("B. 機體與結構檢查...");
        if (!car.isWingLockSensorOk()) return fail("主翼鎖感測器異常");
        if (!car.isPropellerClear()) return fail("螺旋槳有異物");
        if (!car.isStructuralOk()) return fail("結構感測器異常");
        return pass("結構檢查通過");
    }
    private boolean checkC_Power() {
        System.out.println("C. 電力與動力系統...");
        if (!car.isBMSOk()) return fail("BMS 告警");
        if (!car.isPropulsionOk()) return fail("推進器自檢失敗");
        return car.getEMS().runPreflightCheck(); // 委託給 EMS
    }
    private boolean checkD_Sensors() {
        System.out.println("D. 感測器與飛控系統...");
        if (!car.isIMUHealthy()) return fail("IMU (姿態) 異常");
        if (car.getGnssSatellites() < 8) return fail("GNSS 訊號微弱");
        if (!car.isFlightControlOk()) return fail("飛行控制器錯誤");
        return pass("感測器與飛控通過");
    }
    private boolean checkE_WeightAndBalance() {
        System.out.println("E. 重量與重心 (W&B Check)...");
        if (car.getCurrentWeight() > car.getMaxTakeoffWeight()) return fail("超出最大起飛重量");
        return pass("重量與重心通過");
    }
    private boolean checkF_Cabin() {
        System.out.println("F. 駕駛艙 / 內部確認...");
        if (!car.isPassengerBelted()) return fail("乘客安全帶未繫好");
        return pass("艙內確認通過");
    }

    private boolean checkL_A_GroundEnv() { return pass("A. 降落區狀態良好"); }
    private boolean checkL_B_Power() {
        System.out.println("B. 動力與電池...");
        return car.getEMS().checkLandingReserve(); // 委託給 EMS
    }
    private boolean checkL_C_Sensors() { return pass("C. 飛控與感測器正常"); }
    private boolean checkL_D_Structure() { return pass("D. 車身與機構正常"); }
}

// --- 輔助類別 3：EmergencyProtectionSystem ---
class EmergencyProtectionSystem {
    private FlyingCar car;
    public EmergencyProtectionSystem(FlyingCar car) { this.car = car; }

    public boolean checkForFatalErrors() {
        if (car.getCurrentAltitude() <= 0) return false;
        if (!car.isPropulsionOk()) return trigger("致命級動力問題");
        if (car.getBatteryCharge() < 7.0) return trigger("電力危機 (電量 < 7%)");
        if (!car.isStructuralOk()) return trigger("結構損壞");
        if (!car.isFlightControlOk()) return trigger("飛控失效");
        return false;
    }

    private boolean trigger(String reason) {
        System.out.println("\n--- [!!! 警告 !!!] ---");
        System.out.println("偵測到致命級故障：" + reason);
        System.out.println("--- 啟動 [預墜落模式] (Pre-Crash Mode) ---");
        return true;
    }

    public void activatePreCrashSequence() throws InterruptedException {
        System.out.println("\n--- (0-2 秒) 預墜落模式 ---");
        simulateProcess("[EPS] (II-1) 切入「姿態穩定保護」", 100);
        simulateProcess("[EPS] (II-2) 自動搜尋最佳迫降區...", 100);
        simulateProcess("[EPS] (II-3) 廣播求救訊號 (Beacon)...", 100);
        System.out.println("\n--- (3-5 秒) 減速與準備 ---");
        simulateProcess("[EPS] (III-4) 啟動「受控下降模式」", 200);
        System.out.println("\n--- (5-10 秒) 最後安全動作 ---");
        if (car.getCurrentAltitude() >= 80) {
            simulateProcess("[EPS] (IV-6) 高度足夠，啟動「整機降落傘」", 300);
        } else {
            simulateProcess("[EPS] (IV-7) 高度不足！啟動「地面衝擊減損模式」", 300);
        }
        System.out.println("\n--- (撞擊前 1-2 秒) 衝擊防護 ---");
        System.out.println("[EPS] (車內語音) 衝擊防護啟動中...");
        simulateProcess("[EPS] (V-8) 自動斷電 (切斷高壓電系統)", 100);
        car.emergencyPowerCut();
        simulateProcess("[EPS] (V-9) 座艙保護 (緊縮安全帶)", 100);
        System.out.println("\n--- (撞擊後 0-5 秒) 自動救援 ---");
        simulateProcess("[EPS] (VI-11) 自動解鎖車門", 100);
        simulateProcess("[EPS] (VI-12) 自動啟用求救信標 (發送GPS位置)", 100);
    }

    private void simulateProcess(String message, int milliseconds) throws InterruptedException {
        System.out.println(message);
        Thread.sleep(milliseconds);
    }
}

// --- 實體類別 2 (對照組：特斯拉 - 完整版) ---
class RegularCar extends Vehicle implements Drivable {
    private boolean isLudicrousMode = false;
    private boolean isDriving = false;
    public RegularCar(String model) { super(model); }
    public void toggleLudicrousMode() {
        this.isLudicrousMode = !this.isLudicrousMode;
        System.out.println(getModel() + " Plaid 模式 (Ludicrous Mode) " + (isLudicrousMode ? "啟動" : "關閉") + "!");
    }
    @Override
    public void drive() {
        if (!isSystemOnline()) {
            System.out.println(getModel() + " 駕駛失敗：請先啟動系統。");
            return;
        }
        double consumption = isLudicrousMode ? 8.0 : 3.0;
        int speed = isLudicrousMode ? 210 : 120;
        String modeStr = isLudicrousMode ? "[Plaid 模式]" : "[標準模式]";
        if (consumeBattery(consumption)) {
            this.isDriving = true;
            this.currentSpeed = speed;
            System.out.println(getModel() + " " + modeStr + " 正在高速公路上行駛，時速 " + currentSpeed + " km/h。");
        } else {
            System.out.println(getModel() + " 駕駛失敗：電力不足。");
        }
    }
    @Override
    public void stopDriving() {
        if (isDriving) {
            this.isDriving = false;
            this.currentSpeed = 0;
            regainBattery(2.0); // 普通車的動能回收
            System.out.println(getModel() + " 停車。");
        }
    }
}

// --- 實體類別 3 (對照組：飛機 - 完整版) ---
class Airplane extends Vehicle implements Flyable {
    private boolean isFlying;
    public Airplane(String model) { super(model); this.isFlying = false; }
    @Override
    public void takeOff() {
        if (!isSystemOnline()) {
            System.out.println(getModel() + " 起飛失敗：請先啟動系統。");
            return;
        }
        if (consumeBattery(30.0)) {
            this.isFlying = true;
            this.currentSpeed = 300;
            System.out.println(getModel() + " 正在從跑道起飛...");
        } else {
            System.out.println(getModel() + " 起飛失敗：電力不足。");
        }
    }
    @Override
    public void fly() {
        if (!isFlying) {
            System.out.println(getModel() + " 飛行失敗：請先起飛。");
            return;
        }
        if (consumeBattery(15.0)) {
            this.currentSpeed = 800;
            System.out.println(getModel() + " 正在 10000 公尺高空巡航，時速 " + currentSpeed + " km/h。");
        } else {
            System.out.println(getModel() + " 飛行失敗：電力不足，請求緊急迫降。");
            land();
        }
    }
    @Override
    public void land() {
        if (!isFlying) {
            System.out.println(getModel() + " 降落失敗：不在空中。");
            return;
        }
        this.isFlying = false;
        this.currentSpeed = 0;
        System.out.println(getModel() + " 已降落在機場跑道。");
    }
}


// --- 主程式 (整合所有 5 個測試情境) ---
public class Main {
    public static void main(String[] args) throws InterruptedException {

        System.out.println("--- 測試 1：對照組 (RegularCar) ---");
        RegularCar tesla = new RegularCar("Tesla Model S Plaid (基準)");
        tesla.powerOn();
        tesla.toggleLudicrousMode();
        tesla.drive();
        tesla.stopDriving();
        tesla.powerOff();

        System.out.println("\n\n--- 測試 2：對照組 (Airplane) ---");
        Airplane boeing = new Airplane("Boeing 787 (Electric Mod)");
        boeing.powerOn();
        boeing.takeOff();
        boeing.fly();
        boeing.land();
        boeing.powerOff();

        System.out.println("\n\n--- 測試 3：正常飛行 (FlyingCar) ---");
        FlyingCar flyTesla = new FlyingCar("Tesla 'Aero' Model T");
        flyTesla.powerOn();

        System.out.println("\n--- [正常] 階段 1: 啟動飛行模式 (100%) ---");
        flyTesla.requestFlightMode(); // 將通過 C. 電力檢查
        flyTesla.requestTakeOff();    // SOC 100 -> 80

        System.out.println("\n--- [正常] 階段 2: 巡航 ---");
        flyTesla.toggleEcoMode();     // 開啟 E-Mode
        flyTesla.requestFly();        // 節能巡航 (80 -> 72)

        System.out.println("\n--- [正常] 階段 3: 降落與動能回收 ---");
        flyTesla.requestLanding();
        flyTesla.requestGroundMode();
        flyTesla.drive();             // 地面駕駛 (72 -> 71)
        flyTesla.stopDriving();       // 動能回收 (71 -> 73.5)
        System.out.println("[主控] 最終電量：" + String.format("%.1f", flyTesla.getBatteryCharge()) + "%");
        flyTesla.powerOff();

        System.out.println("\n\n--- 測試 4：電力不足 (EMS 檢查) ---");
        FlyingCar lowBatteryCar = new FlyingCar("Test-Low-Battery");
        lowBatteryCar.powerOn();
        lowBatteryCar.simulateLowBattery(50.0); // 模擬電量 50%
        lowBatteryCar.requestFlightMode(); // 將在 C. 電力檢查失敗 (低於 60%)
        lowBatteryCar.powerOff();

        System.out.println("\n\n--- 測試 5：致命故障 (EPS 檢查) ---");
        FlyingCar failCar = new FlyingCar("Test-Failure-01");
        failCar.powerOn();
        failCar.requestFlightMode(); // SOC 100, 通過
        failCar.requestTakeOff();    // SOC 80
        failCar.requestFly();        // SOC 68

        System.out.println("\n--- [主控] 模擬推進器失效... ---");
        failCar.simulateSystemFailure("Propulsion");

        System.out.println("\n--- [主控] 再次巡航 (將觸發 EPS) ---");
        failCar.requestFly();

        System.out.println("\n--- [主控] 檢查車輛是否鎖定 ---");
        failCar.drive(); // 應無效
        failCar.powerOff(); // 應顯示已被 EPS 斷電
    }
}
