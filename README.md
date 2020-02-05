# Vetd

Vetd is a Clojure(Script) web app that helps users discover and compare SaaS products. The backend is built on Hasura (GraphQL) to offer data subscriptions with real-time updates from the connected Postgres database. The frontend is built on Re-frame and Semantic UI.

Special Re-frame subscriptions ([graphql.cljs#L84-L122](https://github.com/Vetd-Inc/vetd-app/blob/5b4d8ffd1eceb294105068d71a02596fa28a3a1f/src/cljs/app/vetd_app/graphql.cljs#L84-L122)) allow you to subscribe to GraphQL with EDN either as a one-time data retrieval, or as a GraphQL Subscription with continued real-time updates over WebSockets.

