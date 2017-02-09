'use strict';
var yeoman = require('yeoman-generator');
var chalk = require('chalk');
var packagejs = require(__dirname + '/../../package.json');

// Stores JHipster variables
var jhipsterVar = { moduleName: 'grpc' };

// Stores JHipster functions
var jhipsterFunc = {};

const PROTO_DIR = 'src/main/proto';

module.exports = yeoman.Base.extend({

    initializing: {
        compose: function (args) {
            this.composeWith('jhipster:modules',
                {
                    options: {
                        jhipsterVar: jhipsterVar,
                        jhipsterFunc: jhipsterFunc
                    }
                },
                this.options.testmode ? { local: require.resolve('generator-jhipster/generators/modules') } : null
            );
        },
        displayLogo: function () {
            // Have Yeoman greet the user.
            this.log('Welcome to the ' + chalk.red('JHipster grpc') + ' generator! ' + chalk.yellow('v' + packagejs.version + '\n'));
        }
    },

    prompting: function () {
        var done = this.async();

        var prompts = [{
            type: 'input',
            name: 'message',
            message: 'Please put something',
            default: 'hello world!'
        }];

        this.prompt(prompts, function (props) {
            this.props = props;
            // To access props later use this.props.someOption;

            done();
        }.bind(this));
    },

    writing: {
        writeTemplates: function () {
            this.baseName = jhipsterVar.baseName;
            this.packageName = jhipsterVar.packageName;
            this.angularAppName = jhipsterVar.angularAppName;
            var javaDir = jhipsterVar.javaDir;

            this.message = this.props.message;

            this.template('_date.proto', PROTO_DIR + '/util/date.proto', this, {});
            this.template('_decimal.proto', PROTO_DIR + '/util/decimal.proto', this, {});
            this.template('_ProtobufUtil.java', javaDir + '/grpc/ProtobufUtil.java');
            jhipsterFunc.addMavenDependency('org.lognet', 'grpc-spring-boot-starter', '2.0.0');
            // Resolve conflict with springfox
            jhipsterFunc.addMavenDependency('com.google.guava', 'guava', '20.0');
            jhipsterFunc.addMavenDependency('io.grpc', 'grpc-protobuf', '1.1.1');
            jhipsterFunc.addMavenDependency('io.grpc', 'grpc-stub', '1.1.1');
            jhipsterFunc.addMavenPlugin('org.xolstice.maven.plugins', 'protobuf-maven-plugin', '0.5.0',
                '                ' +
                '<configuration>' + '\n                ' +
                '    <protocArtifact>com.google.protobuf:protoc:3.1.0:exe:${os.detected.classifier}</protocArtifact>' + '\n                ' +
                '    <pluginId>grpc-java</pluginId>' + '\n                ' +
                '    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.1.1:exe:${os.detected.classifier}</pluginArtifact>' + '\n                ' +
                '</configuration>' + '\n                ' +
                '<executions>' + '\n                ' +
                '    <execution>' + '\n                ' +
                '        <goals>' + '\n                ' +
                '            <goal>compile</goal>' + '\n                ' +
                '            <goal>compile-custom</goal>' + '\n                ' +
                '        </goals>' + '\n                ' +
                '    </execution>' + '\n                ' +
                '</executions>'
            );
            jhipsterFunc.replaceContent('pom.xml', '</defaultGoal>\n        <plugins>',
                '</defaultGoal>' + '\n        ' +
                '<extensions>' + '\n        ' +
                '    <extension>' + '\n        ' +
                '        <groupId>kr.motd.maven</groupId>' + '\n        ' +
                '        <artifactId>os-maven-plugin</artifactId>' + '\n        ' +
                '        <version>1.4.1.Final</version>' + '\n        ' +
                '    </extension>' + '\n        ' +
                '</extensions>' + '\n        ' +
                '<plugins>'
            );
            /*<repositories>
                <repository>
                    <id>jcenter</id>
                    <url>http://jcenter.bintray.com </url>
                </repository>
            </repositories>*/

        },

        registering: function () {
            try {
                jhipsterFunc.registerModule('generator-jhipster-grpc', 'entity', 'post', 'entity', 'Adds support for gRPC and generates gRPC CRUD endpoints');
            } catch (err) {
                this.log(chalk.red.bold('WARN!') + ' Could not register as a jhipster entity post creation hook...\n');
            }
        }
    },

    end: function () {
        this.log('End of grpc generator');
    }
});