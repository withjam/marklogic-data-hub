(:
  Copyright 2012-2016 MarkLogic Corporation

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
:)
xquery version "1.0-ml";

module namespace service = "http://marklogic.com/rest-api/resource/flow";

import module namespace debug = "http://marklogic.com/data-hub/debug"
  at "/com.marklogic.hub/lib/debug-lib.xqy";

import module namespace flow = "http://marklogic.com/data-hub/flow-lib"
  at "/com.marklogic.hub/lib/flow-lib.xqy";

import module namespace perf = "http://marklogic.com/data-hub/perflog-lib"
  at "/com.marklogic.hub/lib/perflog-lib.xqy";

declare namespace rapi = "http://marklogic.com/rest-api";

declare namespace hub = "http://marklogic.com/data-hub";

declare option xdmp:mapping "false";

(:~
 : Entry point for java to get flow(s).
 :
 : if the "flow-name" param is given then return a flow. Otherwise
 : return all flows.
 :
 :)
declare function get(
  $context as map:map,
  $params  as map:map
  ) as document-node()*
{
  debug:dump-env("GET FLOW"),

  perf:log('/v1/resources/flow:get', function() {
    document {
      let $entity-name := map:get($params, "entity-name")
      let $flow-name := map:get($params, "flow-name")
      let $flow-type := map:get($params, "flow-type")
      let $resp :=
        if ($flow-name) then
          let $flow := flow:get-flow($entity-name, $flow-name, $flow-type)
          return
            if (fn:exists($flow)) then $flow
            else
              fn:error((),"RESTAPI-SRVEXERR", (404, "Not Found", "The requested flow was not found"))
        else
          flow:get-flows($entity-name)
      return
       $resp
    }
  })
};

(:~
 : Entry point for java to run a flow.
 :
 : The flow xml is provided in the request body
 :)
declare function post(
  $context as map:map,
  $params  as map:map,
  $input   as document-node()*
  ) as document-node()*
{
  debug:dump-env("RUN FLOW"),

  perf:log('/v1/resources/flow:post', function() {
    let $flow as element(hub:flow) := $input/hub:flow
    let $options := map:new((
      map:entry("entity", $flow/hub:entity/fn:data()),
      map:entry("flow", $flow/hub:name/fn:data()),
      map:entry("flowType", $flow/hub:type/fn:data())
    ))
    for $identifier in map:get($params, "identifier")
    let $_ := flow:run-flow($flow, $identifier, $options)
    return
      document { () }
  })
};

declare function delete(
  $context as map:map,
  $params  as map:map
  ) as document-node()?
{
  debug:dump-env("INVALIDATE FLOW CACHES"),

  perf:log('/v1/resources/flow:delete', function() {
    let $_ := flow:invalidate-flow-caches()
    return
      document { () }
  })
};
