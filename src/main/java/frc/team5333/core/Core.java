package frc.team5333.core;

import edu.wpi.first.wpilibj.command.Scheduler;
import frc.team5333.core.commands.CommandClearConfigs;
import frc.team5333.core.commands.CommandSpeedTest;
import frc.team5333.core.control.ControlLoopManager;
import frc.team5333.core.control.Operator;
import frc.team5333.core.control.TransientControls;
import frc.team5333.core.control.strategy.StrategyController;
import frc.team5333.core.control.strategy.StrategyOperator;
import frc.team5333.core.data.MatchInfo;
import frc.team5333.core.events.StateChangeEvent;
import frc.team5333.core.hardware.IO;
import frc.team5333.core.network.NetworkHub;
import frc.team5333.core.systems.Systems;
import frc.team5333.core.vision.VisionNetwork;
import frc.team5333.lib.events.EventBus;
import jaci.openrio.toast.core.StateTracker;
import jaci.openrio.toast.core.command.CommandBus;
import jaci.openrio.toast.core.loader.annotation.Priority;
import jaci.openrio.toast.lib.log.Logger;
import jaci.openrio.toast.lib.module.IterativeModule;
import jaci.openrio.toast.lib.module.ModuleConfig;

/**
 * The Core class of Team 5333's Stronghold code. This class serves to startup other parts of the code, as well
 * as delegate periodic functions to the appropriate handler.
 *
 * @author Jaci
 */
public class Core extends IterativeModule {

    public static ModuleConfig config;
    public static Logger logger;

    @Override
    public String getModuleName() {
        return "5333-Core";
    }

    @Override
    public String getModuleVersion() {
        return "0.1.0";
    }

    @Override
    @Priority(level = Priority.Level.HIGHEST)
    public void prestart() {
        logger = new Logger("5333-Stronghold", Logger.ATTR_DEFAULT);
        config = new ModuleConfig("5333-Stronghold");

        NetworkHub.INSTANCE.start();

        ControlLoopManager.start();

        Operator.init();
        IO.init();
        Systems.init();

        MatchInfo.load();

        VisionNetwork.INSTANCE.init();

        TransientControls.init();
        StateTracker.addTicker((s) -> { TransientControls.tick(); });
        StateTracker.addTicker((s) -> { Scheduler.getInstance().run(); });
        StateTracker.addTransition((o,n) -> { EventBus.INSTANCE.raiseEvent(new StateChangeEvent(o,n)); });

        CommandBus.registerCommand(new CommandClearConfigs());
        CommandBus.registerCommand(new CommandSpeedTest());
    }

    public void autonomousInit() { }

    @Override
    public void autonomousPeriodic() {
        StrategyController.INSTANCE.tickSlow();
    }

    @Override
    public void teleopInit() {
        StrategyController.INSTANCE.setStrategy(new StrategyOperator());
    }

    @Override
    public void teleopPeriodic() {
        StrategyController.INSTANCE.tickSlow();
    }

}