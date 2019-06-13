package io.serialized.samples;

import io.serialized.client.SerializedClientConfig;
import io.serialized.client.aggregate.AggregateClient;
import io.serialized.client.aggregate.Event;
import io.serialized.client.projection.ProjectionClient;
import io.serialized.client.projection.query.ListProjectionQuery;
import io.serialized.client.projection.query.SingleProjectionQuery;
import io.serialized.samples.api.CompleteTodoRequest;
import io.serialized.samples.api.CreateTodoListRequest;
import io.serialized.samples.api.CreateTodoRequest;
import io.serialized.samples.domain.TodoList;
import io.serialized.samples.domain.TodoListState;
import io.serialized.samples.domain.event.TodoAdded;
import io.serialized.samples.domain.event.TodoCompleted;
import io.serialized.samples.domain.event.TodoListCompleted;
import io.serialized.samples.domain.event.TodoListCreated;
import spark.QueryParamsMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.serialized.client.aggregate.AggregateClient.aggregateClient;
import static io.serialized.client.projection.EventSelector.eventSelector;
import static io.serialized.client.projection.Function.*;
import static io.serialized.client.projection.ProjectionDefinition.singleProjection;
import static io.serialized.client.projection.RawData.rawData;
import static io.serialized.client.projection.TargetFilter.targetFilter;
import static io.serialized.client.projection.TargetSelector.targetSelector;
import static io.serialized.samples.JsonConverter.fromJson;
import static org.apache.commons.lang3.StringUtils.defaultString;
import static spark.Spark.*;

public class TodoService {

  private static final String LIST_TYPE = "list";
  private static final String LISTS_PROJECTION = "lists";

  private static final AggregateClient<TodoListState> listClient;
  private static final ProjectionClient projectionClient;

  static {
    SerializedClientConfig serializedClientConfig = getConfig();

    // Register mapping between event types and handler methods
    listClient = aggregateClient(LIST_TYPE, TodoListState.class, serializedClientConfig)
        .registerHandler(TodoListCreated.class, TodoListState::handleTodoListCreated)
        .registerHandler(TodoAdded.class, TodoListState::handleTodoAdded)
        .registerHandler(TodoCompleted.class, TodoListState::handleTodoCompleted)
        .registerHandler(TodoListCompleted.class, TodoListState::handleTodoListCompleted)
        .build();

    projectionClient = ProjectionClient.projectionClient(serializedClientConfig).build();

    // Make sure the projection is configured in Serialized
    projectionClient.createOrUpdate(
        singleProjection(LISTS_PROJECTION)
            .feed(LIST_TYPE)
            .addHandler("TodoListCreated",
                set()
                    .with(targetSelector("name"))
                    .with(eventSelector("name"))
                    .build(),
                set()
                    .with(targetSelector("status"))
                    .with(rawData("EMPTY"))
                    .build(),
                setref()
                    .with(targetSelector("status"))
                    .build()
            )
            .addHandler("TodoAdded",
                prepend()
                    .with(targetSelector("todos"))
                    .build(),
                set()
                    .with(targetSelector("todos[?].status"))
                    .with(targetFilter("[?(@.todoId == $.event.todoId)]"))
                    .with(rawData("IN_PROGRESS")).build(),
                set()
                    .with(targetSelector("status"))
                    .with(rawData("IN_PROGRESS"))
                    .build(),
                setref()
                    .with(targetSelector("status"))
                    .build()
            )
            .addHandler("TodoCompleted",
                set()
                    .with(targetSelector("todos[?].status"))
                    .with(targetFilter("[?(@.todoId == $.event.todoId)]"))
                    .with(rawData("COMPLETED")).build(),
                setref()
                    .with(targetSelector("status"))
                    .build())
            .addHandler("TodoListCompleted",
                set()
                    .with(targetSelector("status"))
                    .with(rawData("COMPLETED"))
                    .build(),
                setref()
                    .with(targetSelector("status"))
                    .build()
            ).build());
  }

  public static void main(String[] args) {
    port(8080);

    before((request, response) -> response.type("application/json"));

    post("/commands/create-list", (request, response) -> {
      // Convert incoming JSON payload to request class
      CreateTodoListRequest req = fromJson(request.body(), CreateTodoListRequest.class);
      // Construct initial state of the domain object
      TodoList todoList = new TodoList(new TodoListState());
      // Execute domain logic
      List<Event> events = todoList.createNew(req.listId, req.name);
      // Store event in Serialized
      listClient.save(req.listId, events);

      return "";
    });

    post("/commands/create-todo", (request, response) -> {
      // Convert incoming JSON payload to request class
      CreateTodoRequest req = fromJson(request.body(), CreateTodoRequest.class);
      // Load current state, update aggregate and save
      listClient.update(req.listId, state -> {
        // Init domain object with current state
        TodoList todoList = new TodoList(state);
        // Execute domain logic
        return todoList.addTodo(req.todoId, req.todoText);
      });

      return "";
    });

    post("/commands/complete-todo", (request, response) -> {
      // Convert incoming JSON payload to request class
      CompleteTodoRequest req = fromJson(request.body(), CompleteTodoRequest.class);
      // Load current state, update aggregate and save
      listClient.update(req.listId, state -> {
        // Init domain object with current state
        TodoList todoList = new TodoList(state);
        // Execute domain logic
        return todoList.completeTodo(req.todoId);
      });

      return "";
    });

    get("/queries/lists", (request, response) -> {
      ListProjectionQuery.Builder builder = new ListProjectionQuery.Builder("lists");
      QueryParamsMap queryParamsMap = request.queryMap("status");
      if (queryParamsMap.hasValue()) {
        return projectionClient.list(builder.reference(queryParamsMap.value()).build(Map.class));
      } else {
        return projectionClient.list(builder.build(Map.class));
      }
    }, new JsonConverter());

    get("/queries/lists/:listId", (request, response) -> {
      String listId = request.params(":listId");
      SingleProjectionQuery query = new SingleProjectionQuery.Builder("lists").id(listId).build(Map.class);
      return projectionClient.query(query);
    }, new JsonConverter());

    exception(IllegalArgumentException.class, (exception, request, response) -> {
      response.status(400);
      response.type("application/json");
      response.body("{\"message\":\"" + exception.getMessage() + "\"}");
    });

  }

  private static SerializedClientConfig getConfig() {
    return SerializedClientConfig.serializedConfig()
        .accessKey(getConfig("SERIALIZED_ACCESS_KEY"))
        .secretAccessKey(getConfig("SERIALIZED_SECRET_ACCESS_KEY")).build();
  }

  private static String getConfig(String key) {
    return Optional.ofNullable(defaultString(System.getenv(key), System.getProperty(key)))
        .orElseThrow(() -> new IllegalStateException("Missing environment property: " + key));
  }

}