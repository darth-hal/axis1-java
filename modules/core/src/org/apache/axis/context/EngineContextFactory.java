package org.apache.axis.context;

import org.apache.axis.deployment.DeploymentEngine;
import org.apache.axis.deployment.DeploymentException;
import org.apache.axis.description.AxisModule;
import org.apache.axis.description.AxisOperation;
import org.apache.axis.description.AxisService;
import org.apache.axis.engine.AxisFault;
import org.apache.axis.engine.AxisSystem;
import org.apache.axis.engine.AxisSystemImpl;
import org.apache.axis.modules.Module;
import org.apache.axis.phaseresolver.PhaseException;
import org.apache.axis.phaseresolver.PhaseResolver;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * Author : Deepal Jayasinghe
 * Date: Apr 19, 2005
 * Time: 10:44:38 AM
 */
public class EngineContextFactory {

    public SystemContext buildEngineContext(String RepositaryName) throws DeploymentException {
        SystemContext engineContext = null;
        try {
            DeploymentEngine deploymentEngine = new DeploymentEngine(RepositaryName);
            AxisSystem configuration = deploymentEngine.load();
            PhaseResolver phaseResolver = new PhaseResolver(configuration);
            engineContext = phaseResolver.buildGlobalChains();
            phaseResolver.buildTranspotsChains();
            initModules(engineContext);
        } catch (AxisFault axisFault) {
            throw new DeploymentException(axisFault.getMessage());
        } catch (PhaseException e) {
            throw new DeploymentException(e.getMessage());
        }
        return engineContext;
    }

    public SystemContext buildClientEngineContext(String axis2home) throws DeploymentException {
        SystemContext engineContext = null;
        try {
            DeploymentEngine deploymentEngine = new DeploymentEngine(axis2home);
            AxisSystem configuration = deploymentEngine.loadClient();
            PhaseResolver phaseResolver = new PhaseResolver(configuration);
            engineContext = phaseResolver.buildGlobalChains();
            phaseResolver.buildTranspotsChains();
            initModules(engineContext);
        } catch (AxisFault axisFault) {
            throw new DeploymentException(axisFault.getMessage());
        } catch (PhaseException e) {
            throw new DeploymentException(e.getMessage());
        }
        return engineContext;
    }

    /**
     * Is used to initilize the modules , if the module needs to so some recovery process
     * it can do inside init and this is differnt form module.engage()
     *
     * @param context
     * @throws DeploymentException
     */


    private void initModules(SystemContext context) throws DeploymentException {
        try {
            HashMap modules = ((AxisSystemImpl) context.getEngineConfig()).getModules();
            Collection col = modules.values();
            for (Iterator iterator = col.iterator(); iterator.hasNext();) {
                AxisModule axismodule = (AxisModule) iterator.next();
                Module module = axismodule.getModule();
                if (module != null) {
                    module.init(context.getEngineConfig());
                }
            }
        } catch (AxisFault e) {
            throw new DeploymentException(e.getMessage());
        }
    }

    public void createChains(AxisService service, AxisSystem system) throws PhaseException {
        try {
            PhaseResolver reolve = new PhaseResolver(system, service);
            reolve.buildchains();
            engageModules(service, system);
        } catch (PhaseException e) {
            throw new PhaseException(e.getMessage());
        } catch (AxisFault axisFault) {
            throw new PhaseException(axisFault.getMessage());
        }
    }

    private void engageModules(AxisService service, AxisSystem context) throws AxisFault {
        ArrayList servicemodules = (ArrayList) service.getModules();
        ArrayList opModules;
        Module module;
        Collection operations = service.getOperations().values();
        for (Iterator iterator = operations.iterator(); iterator.hasNext();) {
            AxisOperation operation = (AxisOperation) iterator.next();
            opModules = (ArrayList) operation.getModules();
            for (int i = 0; i < servicemodules.size(); i++) {
                QName moduleName = (QName) servicemodules.get(i);
                module = context.getModule(moduleName).getModule();
                //todo AxisOperation shoud have a method to get chains
                /*ExecutionChain inchain = new ExecutionChain();
                inchain.addPhases(operation.getPhases(EngineConfiguration.INFLOW));
                module.engage(inchain);*/
            }
            for (int i = 0; i < opModules.size(); i++) {
                QName moduleName = (QName) opModules.get(i);
                module = context.getModule(moduleName).getModule();
            }

        }
    }
}
