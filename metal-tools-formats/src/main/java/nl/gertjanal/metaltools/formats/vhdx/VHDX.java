/**
 * Copyright 2016 Gertjan Al
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.gertjanal.metaltools.formats.vhdx;

import static io.parsingdata.metal.Shorthand.cho;
import static io.parsingdata.metal.Shorthand.con;
import static io.parsingdata.metal.Shorthand.expTrue;
import static io.parsingdata.metal.Shorthand.gtNum;
import static io.parsingdata.metal.Shorthand.last;
import static io.parsingdata.metal.Shorthand.ltNum;
import static io.parsingdata.metal.Shorthand.ref;
import static io.parsingdata.metal.Shorthand.seq;
import static io.parsingdata.metal.Shorthand.sub;
import static nl.gertjanal.metaltools.formats.vhdx.Bat.bat;
import static nl.gertjanal.metaltools.formats.vhdx.FileIdentifier.FILE_IDENTIFIER;
import static nl.gertjanal.metaltools.formats.vhdx.Header.header;
import static nl.gertjanal.metaltools.formats.vhdx.Log.LOGS;
import static nl.gertjanal.metaltools.formats.vhdx.Region.oldRegion;
import static nl.gertjanal.metaltools.formats.vhdx.Region.region;

import io.parsingdata.metal.encoding.ByteOrder;
import io.parsingdata.metal.encoding.Encoding;
import io.parsingdata.metal.token.Token;

/**
 * Virtual Hard Disk X implementation. Based on VHDX Format Specification v1.00:
 * https://www.microsoft.com/en-us/download/details.aspx?id=34750
 *
 * @author Gertjan Al.
 */
public class VHDX {
	private static final Encoding LITTLE_ENDIAN = new Encoding(ByteOrder.LITTLE_ENDIAN);

	private static final Token HEADERS_REGIONS = cho(
		seq(
			sub("header", header(expTrue()), con(0x10000)), // 64KiB
			sub("oldHeader", header(ltNum(last(ref("header.SequenceNumber")))), con(0x20000)), // 128KiB
			sub("region", region(), con(0x30000)), // 192 KiB
			sub("oldRegion", oldRegion(), con(0x40000))), // 256 KiB
		seq(
			sub("oldHeader", header(expTrue()), con(0x10000)), // 64KiB
			sub("header", header(gtNum(last(ref("oldHeader.SequenceNumber")))), con(0x20000)), // 128KiB
			sub("oldRegion", oldRegion(), con(0x30000)), // 192 KiB
			sub("region", region(), con(0x40000)))); // 256 KiB

	public static Token format(final boolean resolveData) {
		return seq(
			LITTLE_ENDIAN,
			FILE_IDENTIFIER,
			HEADERS_REGIONS,
			LOGS,
			bat(resolveData));
	}
}
