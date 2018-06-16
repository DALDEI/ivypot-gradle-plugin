//
// ============================================================================
// (C) Copyright Schalk W. Cronje 2013-2017
//
// This software is licensed under the Apache License 2.0
// See http://www.apache.org/licenses/LICENSE-2.0 for license details
//
// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and limitations under the License.
//
// ============================================================================
//

package org.ysb33r.gradle.ivypot

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Stepwise


/** These fixtures needs to run in the order specified as these tests are expensive in terms of downloads
 * The repository is only created once during the run to save on download time.
 *
 * @author Schalk W. Cronjé
 */
@Stepwise
class OfflineRepositorySyncIntegrationSpec extends Specification {

    static final boolean OFFLINE = System.getProperty('IS_OFFLINE')
    static final File ROOT = new File("${System.getProperty('TESTROOT') ?: new File('./build').absolutePath}/integrationTest/orsis")
    static final File TESTROOT = new File(ROOT,'tests')
    static final File LOCALREPO = new File(ROOT, 'local-offline-repo')

    Project project
    Task syncTask

    void setupSpec() {
        OfflineRepositorySync.DONT_LOOK_FOR_IVY_JAR = true

        if (LOCALREPO.exists()) {
            LOCALREPO.deleteDir()
        }

        LOCALREPO.mkdirs()

    }

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }

        TESTROOT.mkdirs()
        project = ProjectBuilder.builder().withProjectDir(TESTROOT).build()
        project.apply plugin: 'org.ysb33r.ivypot'
    }

    @IgnoreIf({ OFFLINE })
    def "Can we sync from mavenCentral?"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            // tag::example_jcenter[]
            dependencies {
                compile 'commons-io:commons-io:2.4'
            }

            syncRemoteRepositories {
                repositories {
                    mavenCentral()
                }

                repoRoot "${pathToLocalRepo}"
            }
            // end::example_jcenter[]
        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        LOCALREPO.exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/ivy-2.4.xml').exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/commons-io-2.4.jar').exists()
    }

    @IgnoreIf({ OFFLINE })
    def "Honour non-transitive dependencies"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            dependencies {
                compile 'junit:junit:4.12', {
                    transitive = false
                }
            }

            syncRemoteRepositories {
                repositories {
                    mavenCentral()
                }

                repoRoot "${pathToLocalRepo}"
            }
        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        LOCALREPO.exists()
        new File(LOCALREPO, 'junit/junit/4.12/ivy-4.12.xml').exists()
        new File(LOCALREPO, 'junit/junit/4.12/junit-4.12.jar').exists()
        !new File(LOCALREPO, 'org.hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar').exists()
    }

    @IgnoreIf({ OFFLINE })
    def "Can we sync from mavenCentral using a different local artifactPattern?"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            dependencies {
                compile 'commons-io:commons-io:2.4'
            }

            syncRemoteRepositories {
                repositories {
                    mavenCentral()
                }

                repoRoot "${pathToLocalRepo}"

                repoArtifactPattern = '[organisation]/[module]/[revision]/[type]s/[artifact]-[revision](.[ext])'
            }
        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        LOCALREPO.exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/ivy-2.4.xml').exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/jars/commons-io-2.4.jar').exists()
    }

    @IgnoreIf({ OFFLINE })
    def "Two syncs to same folder should not cause an overwrite exceptions"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            dependencies {
                compile 'commons-io:commons-io:2.4'
            }

            syncRemoteRepositories {
                repositories {
                    jcenter()
                    mavenCentral()
                }

                repoRoot "${pathToLocalRepo}"
            }

        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        new File(LOCALREPO, 'commons-io/commons-io/2.4/ivy-2.4.xml').exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/commons-io-2.4.jar').exists()
    }


    @IgnoreIf({ OFFLINE })
    def "Seting includeBuildScriptDependencies means that buildscript configuration will be added"() {

        given:
        def pathToLocalRepo = new File(ROOT,'second-repo')

        project.allprojects {

            buildscript {
                repositories {
                    ivy {
                        url LOCALREPO.toURI()
                        layout 'gradle'
                    }
                }
                dependencies {
                    classpath 'commons-io:commons-io:2.4'
                }
            }


            syncRemoteRepositories {
                repositories {
                    ivy {
                        url LOCALREPO.toURI()
                    }
                }

                repoRoot "${pathToLocalRepo}"

                includeBuildScriptDependencies = true
            }
        }

        project.evaluate()

        Set<Dependency> deps = project.syncRemoteRepositories.dependencies

        expect:
        deps.find { Dependency it -> it.group == 'commons-io' && it.name == 'commons-io' && it.version == '2.4' }
    }

    @Issue('https://github.com/ysb33r/ivypot-gradle-plugin/issues/12')
    @IgnoreIf({ true || OFFLINE })
    def "Can we sync from a rubygems proxy?"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            // tag::example_rubygems]
            dependencies {
                compile 'rubygems:colorize:0.7.7'
            }

            syncRemoteRepositories {
                repositories {
                    maven { url 'http://rubygems.lasagna.io/proxy/maven/releases' }
                }

                repoRoot "${pathToLocalRepo}"
            }
            // end::example_rubygems[]
        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        LOCALREPO.exists()
        new File(LOCALREPO, 'rubygems/colorize/0.7.7/ivys/ivy.xml').exists()
        new File(LOCALREPO, 'rubygems/colorize/0.7.7/gems/colorize.gem').exists()
    }

    @IgnoreIf({ OFFLINE })
    def "Can we sync from Google?"() {

        given:
        def pathToLocalRepo = LOCALREPO

        project.allprojects {

            configurations {
                compile
            }

            // tag::example_jcenter[]
            dependencies {
                compile 'com.android.support.constraint:constraint-layout:1.0.2'
            }

            syncRemoteRepositories {
                repositories {
                    google()
                }

                repoRoot "${pathToLocalRepo}"
            }
            // end::example_jcenter[]
        }

        project.evaluate()
        project.tasks.syncRemoteRepositories.execute()

        expect:
        LOCALREPO.exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/ivy-2.4.xml').exists()
        new File(LOCALREPO, 'commons-io/commons-io/2.4/commons-io-2.4.jar').exists()
    }

}