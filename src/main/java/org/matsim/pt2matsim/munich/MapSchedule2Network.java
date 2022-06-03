package org.matsim.pt2matsim.munich;

import org.apache.commons.compress.utils.Sets;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.run.CreateDefaultPTMapperConfig;
import org.matsim.pt2matsim.run.PublicTransitMapper;

public class MapSchedule2Network {

    public static void main(String[] args) {

        CreateDefaultPTMapperConfig.main(new String[]{ "config.xml"});

        Config config = ConfigUtils.loadConfig(
                "config.xml",
                PublicTransitMappingConfigGroup.createDefaultConfig());
        PublicTransitMappingConfigGroup ptmConfig = ConfigUtils.addOrGetModule(config, PublicTransitMappingConfigGroup.class);

        ptmConfig.setInputNetworkFile("munichNetwork.xml");
        ptmConfig.setInputScheduleFile("munichSchedule.xml");
        ptmConfig.setOutputNetworkFile("munichNetworkMapped.xml.gz");
        ptmConfig.setOutputScheduleFile("munichScheduleMapped.xml");
        ptmConfig.setOutputStreetNetworkFile("munichStreetNetwork.xml.gz");

        ptmConfig.setScheduleFreespeedModes(CollectionUtils.stringToSet("rail, light_rail"));
        config.controler().setLastIteration(3);
        config.controler().setOverwriteFileSetting( OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists );
        config.controler().setOutputDirectory("scenarios/munich/output");
        config.controler().setWriteEventsInterval(1);
        config.network().setInputFile("munichNetworkMapped.xml.gz");
        config.plans().setInputFile("munichPopulation.xml");
        config.qsim().setStartTime(4*60*60);
        config.qsim().setEndTime(26*60*60);
        config.qsim().setFlowCapFactor(0.01);
        config.qsim().setStorageCapFactor(0.01);

        config.qsim().setMainModes(Sets.newHashSet(TransportMode.car,TransportMode.pt));

        config.transit(). setUseTransit(true);
        config.transit().setTransitScheduleFile("munichScheduleMapped.xml");
        config.transit().setVehiclesFile("munichVehicles.xml");
        config.transit().setTransitModes(Sets.newHashSet(TransportMode.pt));

        PlanCalcScoreConfigGroup.ActivityParams home = new PlanCalcScoreConfigGroup.ActivityParams("home");
        home.setTypicalDuration(12 * 60 * 60);
        config.planCalcScore().addActivityParams(home);
        PlanCalcScoreConfigGroup.ActivityParams work = new PlanCalcScoreConfigGroup.ActivityParams("work");
        work.setTypicalDuration(8 * 60 * 60);
        config.planCalcScore().addActivityParams(work);

        StrategyConfigGroup.StrategySettings strategy1 = new StrategyConfigGroup.StrategySettings();
        strategy1.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute);
        strategy1.setWeight(0.15);
        config.strategy().addStrategySettings(strategy1);

        StrategyConfigGroup.StrategySettings strategy2 = new StrategyConfigGroup.StrategySettings();
        strategy2.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta);
        strategy2.setWeight(0.60);
        config.strategy().addStrategySettings(strategy2);

        StrategyConfigGroup.StrategySettings strategy3 = new StrategyConfigGroup.StrategySettings();
        strategy3.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
        strategy3.setWeight(0.25);
        config.strategy().addStrategySettings(strategy3);

        config.strategy().setMaxAgentPlanMemorySize(5);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.9);
        config.vspExperimental().setWritingOutputEvents(true);

        new ConfigWriter(config).write("config.xml");
        PublicTransitMapper.run("config.xml");
    }
}
