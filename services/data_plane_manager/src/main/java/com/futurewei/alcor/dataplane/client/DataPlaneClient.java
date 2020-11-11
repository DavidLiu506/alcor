/*
Copyright 2019 The Alcor Authors.

Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/
package com.futurewei.alcor.dataplane.client;

import com.futurewei.alcor.dataplane.entity.HostGoalState;
import com.futurewei.alcor.schema.Goalstate.GoalState;
import com.futurewei.alcor.schema.Goalstateprovisioner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public interface DataPlaneClient {
    Map<String, List<Goalstateprovisioner.GoalStateOperationReply.GoalStateOperationStatus>>
    createGoalState(GoalState goalState, String hostIp) throws Exception;

    List<Map<String, List<Goalstateprovisioner.GoalStateOperationReply.GoalStateOperationStatus>>>
    createGoalState(List<HostGoalState> hostGoalStates) throws Exception;

    List<Map<String, List<Goalstateprovisioner.GoalStateOperationReply.GoalStateOperationStatus>>>
    updateGoalState(List<HostGoalState> hostGoalStates) throws Exception;

    List<Map<String, List<Goalstateprovisioner.GoalStateOperationReply.GoalStateOperationStatus>>>
    deleteGoalState(List<HostGoalState> hostGoalStates) throws Exception;
}