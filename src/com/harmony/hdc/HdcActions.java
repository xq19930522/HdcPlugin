package com.harmony.hdc;

/**
 * Tools → HDC Idea 菜单下的具体操作。plugin.xml 里用 $ 引用嵌套类。
 */
public final class HdcActions {

    private HdcActions() {
    }

    public static class UninstallAppAction extends AbstractHdcAction {
        public UninstallAppAction() { super(Operation.UNINSTALL); }
    }

    public static class InstallAppAction extends AbstractHdcAction {
        public InstallAppAction() { super(Operation.INSTALL); }
    }

    public static class KillAppAction extends AbstractHdcAction {
        public KillAppAction() { super(Operation.KILL); }
    }

    public static class StartAppAction extends AbstractHdcAction {
        public StartAppAction() { super(Operation.START); }
    }

    public static class RestartAppAction extends AbstractHdcAction {
        public RestartAppAction() { super(Operation.RESTART); }
    }

    public static class ClearAppDataAction extends AbstractHdcAction {
        public ClearAppDataAction() { super(Operation.CLEAR_DATA); }
    }

    public static class ClearAppDataAndRestartAction extends AbstractHdcAction {
        public ClearAppDataAndRestartAction() { super(Operation.CLEAR_DATA_RESTART); }
    }

    public static class StartAppWithDebuggerAction extends AbstractHdcAction {
        public StartAppWithDebuggerAction() { super(Operation.START_DEBUG); }
    }

    public static class RestartAppWithDebuggerAction extends AbstractHdcAction {
        public RestartAppWithDebuggerAction() { super(Operation.RESTART_DEBUG); }
    }

    public static class ClearAppDataAndRestartWithDebuggerAction extends AbstractHdcAction {
        public ClearAppDataAndRestartWithDebuggerAction() { super(Operation.CLEAR_DATA_RESTART_DEBUG); }
    }

    public static class EnableWifiAction extends AbstractHdcAction {
        public EnableWifiAction() { super(Operation.WIFI_ON); }
    }

    public static class DisableWifiAction extends AbstractHdcAction {
        public DisableWifiAction() { super(Operation.WIFI_OFF); }
    }

    public static class EnableMobileDataAction extends AbstractHdcAction {
        public EnableMobileDataAction() { super(Operation.DATA_ON); }
    }

    public static class DisableMobileDataAction extends AbstractHdcAction {
        public DisableMobileDataAction() { super(Operation.DATA_OFF); }
    }

    public static class GetDeviceUdidAction extends AbstractHdcAction {
        public GetDeviceUdidAction() { super(Operation.GET_UDID); }
    }
}
