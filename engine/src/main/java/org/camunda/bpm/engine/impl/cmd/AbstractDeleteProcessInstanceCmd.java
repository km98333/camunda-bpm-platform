/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.cmd;

import org.camunda.bpm.engine.BadUserRequestException;
import org.camunda.bpm.engine.history.UserOperationLogEntry;
import org.camunda.bpm.engine.impl.cfg.CommandChecker;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionManager;
import org.camunda.bpm.engine.impl.persistence.entity.PropertyChange;

import java.util.Collections;

import static org.camunda.bpm.engine.impl.util.EnsureUtil.ensureNotNull;

/**
 * Created by aakhmerov on 16.09.16.
 * <p>
 * Provide common logic for process instance deletion operations.
 * Permissions checking and single process instance removal included.
 */
public abstract class AbstractDeleteProcessInstanceCmd {

  protected boolean externallyTerminated;
  protected String deleteReason;
  protected boolean skipCustomListeners;

  protected void checkDeleteProcessInstance(ExecutionEntity execution, CommandContext commandContext) {
    for (CommandChecker checker : commandContext.getProcessEngineConfiguration().getCommandCheckers()) {
      checker.checkDeleteProcessInstance(execution);
    }
  }

  protected void deleteProcessInstance(
      CommandContext commandContext,
      String processInstanceId,
      String deleteReason,
      boolean skipCustomListeners,
      boolean externallyTerminated) {
    ensureNotNull(BadUserRequestException.class, "processInstanceId is null", "processInstanceId", processInstanceId);

    // fetch process instance
    ExecutionManager executionManager = commandContext.getExecutionManager();
    final ExecutionEntity execution = executionManager.findExecutionById(processInstanceId);

    ensureNotNull(BadUserRequestException.class, "No process instance found for id '" + processInstanceId + "'", "processInstance", execution);

    checkDeleteProcessInstance(execution, commandContext);

    // delete process instance
    commandContext
        .getExecutionManager()
        .deleteProcessInstance(processInstanceId, deleteReason, false, skipCustomListeners, externallyTerminated);

    // create user operation log
    commandContext.getOperationLogManager()
        .logProcessInstanceOperation(UserOperationLogEntry.OPERATION_TYPE_DELETE, processInstanceId,
            null, null, Collections.singletonList(PropertyChange.EMPTY_CHANGE));
  }

}