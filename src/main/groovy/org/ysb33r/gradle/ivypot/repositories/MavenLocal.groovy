//
// ============================================================================
// (C) Copyright Schalk W. Cronje 2013-2018
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

package org.ysb33r.gradle.ivypot.repositories

import groovy.xml.MarkupBuilder

/**
 * @since 1.0
 */
class MavenLocal extends MavenArtifactRepository {

    MavenLocal() {
        super()
        url = "${new File(System.getProperty('user.home')).absoluteFile.toURI()}.m2/repository/"
    }

    @Override
    void writeTo(MarkupBuilder builder) {
        builder.ibiblio(
                name:name,
                root: url,
                m2compatible: true,
                checkmodified: true,
                changingPattern:'.*',
                changingMatcher: 'regexp'
        )
    }
}
