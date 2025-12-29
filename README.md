Java-FlyingCar
會飛的汽車
此專案為Java學習過程中的實作練習。
作者 Jocelyn Wang

這個專案包含我設計的所有8個類別和介面：
1. Drivable (介面)
2. Flyable (介面)
3. Vehicle (抽象基礎類別)
4. EnergyManagementSystem (EMS - 能源管理)
5. FlightChecklist (SOP - 起降檢查)
6. EmergencyProtectionSystem (EPS - 緊急防護)
7. FlyingCar (主角 - 整合所有系統)
8. RegularCar (對照組 - 特斯拉)
9. Airplane (對照組 - 飛機)

而 Main 主程式包含所有的測試情境：

測試 1: RegularCar 對照組。

測試 2: Airplane 對照組。

測試 3: FlyingCar 的正常飛行流程 (SOP/EMS 檢查)。

測試 4: FlyingCar 的電力不足測試 (EMS 檢查)。

測試 5: FlyingCar 的致命故障測試 (EPS 檢查)。
