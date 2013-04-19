/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb.impl;

import org.mongodb.Codec;
import org.mongodb.Decoder;
import org.mongodb.Document;
import org.mongodb.Encoder;
import org.mongodb.MongoConnector;
import org.mongodb.MongoNamespace;
import org.mongodb.ServerAddress;
import org.mongodb.ServerConnectorManager;
import org.mongodb.async.SingleResultCallback;
import org.mongodb.command.MongoCommand;
import org.mongodb.operation.MongoFind;
import org.mongodb.operation.MongoGetMore;
import org.mongodb.operation.MongoInsert;
import org.mongodb.operation.MongoKillCursor;
import org.mongodb.operation.MongoRemove;
import org.mongodb.operation.MongoReplace;
import org.mongodb.operation.MongoUpdate;
import org.mongodb.result.CommandResult;
import org.mongodb.result.QueryResult;
import org.mongodb.result.ServerCursor;
import org.mongodb.result.WriteResult;

import java.util.List;
import java.util.concurrent.Future;

public class DelegatingMongoConnector implements MongoConnector {
    private final ServerConnectorManager connectorManager;

    public DelegatingMongoConnector(final ServerConnectorManager connectorManager) {
        this.connectorManager = connectorManager;
    }

    public ServerConnectorManager getConnectorManager() {
        return connectorManager;
    }

    @Override
    public CommandResult command(final String database, final MongoCommand commandOperation,
                                 final Codec<Document> codec) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(commandOperation.getReadPreference()).getConnection();
        try {
            return connector.command(database, commandOperation, codec);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> QueryResult<T> query(final MongoNamespace namespace, final MongoFind find,
                                    final Encoder<Document> queryEncoder, final Decoder<T> resultDecoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(find.getReadPreference()).getConnection();
        try {
            return connector.query(namespace, find, queryEncoder, resultDecoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> QueryResult<T> getMore(final MongoNamespace namespace, final MongoGetMore getMore,
                                      final Decoder<T> resultDecoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForServer(getMore.getServerCursor().getAddress()).getConnection();
        try {
            return connector.getMore(namespace, getMore, resultDecoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public void killCursors(final MongoKillCursor killCursor) {
        for (ServerCursor cursor : killCursor.getServerCursors()) {
            MongoPoolableConnector connector = connectorManager.getConnectionManagerForServer(cursor.getAddress()).getConnection();
            try {
                connector.killCursors(new MongoKillCursor(cursor));
            } finally {
                connector.release();
            }
        }
    }

    @Override
    public <T> WriteResult insert(final MongoNamespace namespace, final MongoInsert<T> insert, final Encoder<T> encoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.insert(namespace, insert, encoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public WriteResult update(final MongoNamespace namespace, final MongoUpdate update, final Encoder<Document> queryEncoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.update(namespace, update, queryEncoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> WriteResult replace(final MongoNamespace namespace, final MongoReplace<T> replace,
                                   final Encoder<Document> queryEncoder, final Encoder<T> encoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.replace(namespace, replace, queryEncoder, encoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public WriteResult remove(final MongoNamespace namespace, final MongoRemove remove, final Encoder<Document> queryEncoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.remove(namespace, remove, queryEncoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public Future<CommandResult> asyncCommand(final String database, final MongoCommand commandOperation,
                                              final Codec<Document> codec) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(commandOperation.getReadPreference()).getConnection();
        try {
            return connector.asyncCommand(database, commandOperation, codec);
        } finally {
            connector.release();
        }
    }

    @Override
    public void asyncCommand(final String database, final MongoCommand commandOperation, final Codec<Document> codec,
                             final SingleResultCallback<CommandResult> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(commandOperation.getReadPreference()).getConnection();
        try {
            connector.asyncCommand(database, commandOperation, codec, callback);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> Future<QueryResult<T>> asyncQuery(final MongoNamespace namespace, final MongoFind find,
                                                 final Encoder<Document> queryEncoder, final Decoder<T> resultDecoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(find.getReadPreference()).getConnection();
        try {
            return connector.asyncQuery(namespace, find, queryEncoder, resultDecoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> void asyncQuery(final MongoNamespace namespace, final MongoFind find, final Encoder<Document> queryEncoder,
                               final Decoder<T> resultDecoder, final SingleResultCallback<QueryResult<T>> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForRead(find.getReadPreference()).getConnection();
        try {
            connector.asyncQuery(namespace, find, queryEncoder, resultDecoder, callback);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> Future<QueryResult<T>> asyncGetMore(final MongoNamespace namespace, final MongoGetMore getMore,
                                                   final Decoder<T> resultDecoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForServer(getMore.getServerCursor().getAddress()).getConnection();
        try {
            return connector.asyncGetMore(namespace, getMore, resultDecoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> void asyncGetMore(final MongoNamespace namespace, final MongoGetMore getMore, final Decoder<T> resultDecoder,
                                 final SingleResultCallback<QueryResult<T>> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForServer(getMore.getServerCursor().getAddress()).getConnection();
        connector.asyncGetMore(namespace, getMore, resultDecoder, callback);
    }

    @Override
    public <T> Future<WriteResult> asyncInsert(final MongoNamespace namespace, final MongoInsert<T> insert,
                                               final Encoder<T> encoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.asyncInsert(namespace, insert, encoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> void asyncInsert(final MongoNamespace namespace, final MongoInsert<T> insert,
                                final Encoder<T> encoder,
                                final SingleResultCallback<WriteResult> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            connector.asyncInsert(namespace, insert, encoder, callback);
        } finally {
            connector.release();
        }
    }

    @Override
    public Future<WriteResult> asyncUpdate(final MongoNamespace namespace, final MongoUpdate update,
                                           final Encoder<Document> queryEncoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.asyncUpdate(namespace, update, queryEncoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public void asyncUpdate(final MongoNamespace namespace, final MongoUpdate update,
                            final Encoder<Document> queryEncoder,
                            final SingleResultCallback<WriteResult> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            connector.asyncUpdate(namespace, update, queryEncoder, callback);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> Future<WriteResult> asyncReplace(final MongoNamespace namespace, final MongoReplace<T> replace,
                                                final Encoder<Document> queryEncoder,
                                                final Encoder<T> encoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.asyncReplace(namespace, replace, queryEncoder, encoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public <T> void asyncReplace(final MongoNamespace namespace, final MongoReplace<T> replace,
                                 final Encoder<Document> queryEncoder, final Encoder<T> encoder,
                                 final SingleResultCallback<WriteResult> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            connector.asyncReplace(namespace, replace, queryEncoder, encoder, callback);
        } finally {
            connector.release();
        }
    }

    @Override
    public Future<WriteResult> asyncRemove(final MongoNamespace namespace, final MongoRemove remove,
                                           final Encoder<Document> queryEncoder) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        try {
            return connector.asyncRemove(namespace, remove, queryEncoder);
        } finally {
            connector.release();
        }
    }

    @Override
    public void asyncRemove(final MongoNamespace namespace, final MongoRemove remove,
                            final Encoder<Document> queryEncoder,
                            final SingleResultCallback<WriteResult> callback) {
        MongoPoolableConnector connector = connectorManager.getConnectionManagerForWrite().getConnection();
        connector.asyncRemove(namespace, remove, queryEncoder, callback);
    }

    @Override
    public void close() {
        connectorManager.close();
    }

    @Override
    public List<ServerAddress> getServerAddressList() {
        return connectorManager.getAllServerAddresses();
    }

}