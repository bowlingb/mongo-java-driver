/*
 * Copyright (c) 2008-2014 MongoDB, Inc.
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

package org.mongodb.operation

import org.mongodb.Document
import org.mongodb.Fixture
import org.mongodb.FunctionalSpecification
import org.mongodb.MongoDuplicateKeyException
import org.mongodb.codecs.DocumentCodec
import spock.lang.FailsWith

import static java.util.Arrays.asList
import static org.junit.Assume.assumeTrue
import static org.mongodb.Fixture.getSession
import static org.mongodb.WriteConcern.ACKNOWLEDGED
import static org.mongodb.WriteConcern.UNACKNOWLEDGED

class InsertOperationSpecification extends FunctionalSpecification {
    def 'should return correct result'() {
        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        def result = op.execute(getSession())

        then:
        result.wasAcknowledged()
        result.count == 0
        result.upsertedId == null
        !result.isUpdateOfExisting()
    }

    def 'should return correct result asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        def result = op.executeAsync(getSession()).get()

        then:
        result.wasAcknowledged()
        result.count == 0
        result.upsertedId == null
        !result.isUpdateOfExisting()
    }

    def 'should insert a single document'() {
        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        op.execute(getSession())

        then:
        asList(insert.getDocument()) == getCollectionHelper().find()
    }

    def 'should insert a single document asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        op.executeAsync(getSession()).get()

        then:
        asList(insert.getDocument()) == getCollectionHelper().find()
    }

    def 'should insert a large number of documents'() {
        given:
        def inserts = []
        for (i in 1..1001) {
            inserts += new InsertRequest<Document>(new Document('_id', i))
        }
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, inserts.toList(), new DocumentCodec())


        when:
        op.execute(getSession())

        then:
        getCollectionHelper().count() == 1001
    }

    def 'should insert a large number of documents asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def inserts = []
        for (i in 1..1001) {
            inserts += new InsertRequest<Document>(new Document('_id', i))
        }
        def op = new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, inserts.toList(), new DocumentCodec())


        when:
        op.executeAsync(getSession()).get()

        then:
        getCollectionHelper().count() == 1001
    }

    def 'should return null CommandResult with unacknowledged WriteConcern'() {
        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, UNACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        def result = op.execute(getSession())

        then:
        !result.wasAcknowledged()
    }

    @FailsWith(NullPointerException)
    def 'should return null CommandResult with unacknowledged WriteConcern asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def insert = new InsertRequest<Document>(new Document('_id', 1))
        def op = new InsertOperation<Document>(getNamespace(), true, UNACKNOWLEDGED, asList(insert), new DocumentCodec())

        when:
        def result = op.executeAsync(getSession()).get()

        then:
        !result.wasAcknowledged()
    }

    def 'should insert a batch at The limit of the batch size'() {
        given:
        byte[] hugeByteArray = new byte[1024 * 1024 * 16 - 2127];
        byte[] smallerByteArray = new byte[1024 * 16 + 1980];

        def documents = [
                new InsertRequest<Document>(new Document('bytes', hugeByteArray)),
                new InsertRequest<Document>(new Document('bytes', smallerByteArray))
        ]

        when:
        new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, documents.toList(), new DocumentCodec())
                .execute(getSession())

        then:
        getCollectionHelper().count() == 2
    }

    def 'should insert a batch at The limit of the batch size asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        byte[] hugeByteArray = new byte[1024 * 1024 * 16 - 2127];
        byte[] smallerByteArray = new byte[1024 * 16 + 1980];

        def documents = [
                new InsertRequest<Document>(new Document('bytes', hugeByteArray)),
                new InsertRequest<Document>(new Document('bytes', smallerByteArray))
        ]

        when:
        new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, documents.toList(), new DocumentCodec())
                .executeAsync(getSession()).get()

        then:
        getCollectionHelper().count() == 2
    }

    def 'should continue on error when continuing on error'() {
        given:
        def documents = [
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 2)),
        ]

        when:
        new InsertOperation<Document>(getNamespace(), false, ACKNOWLEDGED, documents, new DocumentCodec())
                .execute(getSession())

        then:
        thrown(MongoDuplicateKeyException)
        getCollectionHelper().count() == 2
    }

    def 'should continue on error when continuing on error asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def documents = [
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 2)),
        ]

        when:
        new InsertOperation<Document>(getNamespace(), false, ACKNOWLEDGED, documents, new DocumentCodec())
                .executeAsync(getSession()).get()

        then:
        thrown(MongoDuplicateKeyException)
        getCollectionHelper().count() == 2
    }

    def 'should not continue on error when not continuing on error'() {
        given:
        def documents = [
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 2)),
        ]

        when:
        new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, documents, new DocumentCodec())
                .execute(getSession())

        then:
        thrown(MongoDuplicateKeyException)
        getCollectionHelper().count() == 1
    }

    def 'should not continue on error when not continuing on error asynchronously'() {
        assumeTrue(Fixture.mongoClientURI.options.isAsyncEnabled())

        given:
        def documents = [
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 1)),
                new InsertRequest<Document>(new Document('_id', 2)),
        ]

        when:
        new InsertOperation<Document>(getNamespace(), true, ACKNOWLEDGED, documents, new DocumentCodec())
                .executeAsync(getSession()).get()

        then:
        thrown(MongoDuplicateKeyException)
        getCollectionHelper().count() == 1
    }
}